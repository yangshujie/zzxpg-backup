import json
from agi.stk12.stkutil import *
from agi.stk12.stkengine import STKEngine
from datetime import datetime, timedelta
import time
import os
import agi
from agi.stk12.stkdesktop import STKDesktop
from agi.stk12.stkobjects import *
from agi.stk12.vgt import *
from Attitude_response_control import *


# 执行你的代码


def merge_intervals(intervals):
    """合并重叠或连续的时间间隔"""
    if not intervals:
        return intervals
    intervals.sort(key=lambda x: x[0])
    merged = [intervals[0]]
    for current in intervals:
        previous = merged[-1]
        if current[0] <= previous[1]:
            merged[-1] = (previous[0], max(previous[1], current[1]))
        else:
            merged.append(current)
    return merged

def parse_datetime_with_ms(date_str):
    date_str = date_str[:26]  # Truncate to microseconds if necessary
    for fmt in ("%d %b %Y %H:%M:%S.%f", "%d %b %Y %H:%M:%S"):
        try:
            return datetime.strptime(date_str, fmt)
        except ValueError:
            continue
    raise ValueError(f"Date string '{date_str}' does not match expected formats")


# 计算区域覆盖百分比,覆盖间隔/点目标
def run_coverage_analysis(config_path, scenario_name, start_time_str, stop_time_str, step_time):
    python_start_time = time.time()
    # 启动STK应用程序
    stk = STKEngine.StartApplication(noGraphics=False)  # 可选地，noGraphics = True
    root = stk.NewObjectRoot()
    root.NewScenario(scenario_name)

    # 设置场景时间
    root.CurrentScenario.SetTimePeriod(start_time_str, stop_time_str)
    root.CurrentScenario.Epoch = start_time_str


    print(f"Scenario Start Time: {root.CurrentScenario.StartTime}")
    print(f"Scenario Stop Time: {root.CurrentScenario.StopTime}")

    # 读取配置文件
    with open(config_path, 'r') as config_file:
        config_data = json.load(config_file)

    # 创建一个新的星座
    constellation = root.CurrentScenario.Children.New(AgESTKObjectType.eConstellation, 'MyConstellation')

    result = []
    # 根据配置文件创建卫星并设置初始状态
    satellites = []
    sensorList = []
    for satellite_data in config_data["satellites"]:
        satellite_name = satellite_data["name"]
        satellite = root.CurrentScenario.Children.New(AgESTKObjectType.eSatellite, satellite_name)  # eSatellite
        constellation.Objects.AddObject(satellite)
        satellites.append(satellite)

        # 设置初始状态
        keplerian = satellite.Propagator.InitialState.Representation.ConvertTo(
            AgEOrbitStateType.eOrbitStateClassical)  # eOrbitStateClassical

        keplerian.SizeShapeType = 0  # eSizeShapeAltitude, 改变形状类型为高度
        keplerian.LocationType = 5  # eLocationTrueAnomaly, 使用真近点角
        keplerian.Orientation.AscNodeType = 0  # eAscNodeLAN, 使用升交点经度

        keplerian.SizeShape.PerigeeAltitude = satellite_data["perigeeAltitude"]
        keplerian.SizeShape.ApogeeAltitude = satellite_data["apogeeAltitude"]
        keplerian.Orientation.Inclination = satellite_data["inclination"]
        keplerian.Orientation.ArgOfPerigee = satellite_data["argOfPerigee"]
        keplerian.Orientation.AscNode.Value = satellite_data["ascNode"]
        keplerian.Location.Value = satellite_data["trueAnomaly"]

        satellite.Propagator.InitialState.Representation.Assign(keplerian)
        satellite.Propagator.Propagate()

        # 输出卫星参数以进行调试
        # print(f"卫星 {satellite_name} 已创建，参数如下:")
        # print(f"近地点高度: {satellite_data['perigeeAltitude']} km")
        # print(f"远地点高度: {satellite_data['apogeeAltitude']} km")
        # print(f"倾角: {satellite_data['inclination']} 度")
        # print(f"近地点参数: {satellite_data['argOfPerigee']} 度")
        # print(f"升交点: {satellite_data['ascNode']} 度")
        # print(f"真近点角: {satellite_data['trueAnomaly']} 度")

        # 设置传感器
        sensor = satellite.Children.New(AgESTKObjectType.eSensor, f"{satellite_name}_Sensor")
        sensor.CommonTasks.SetPatternSimpleConic(satellite_data["coneAngle"], 0.1)  # 设置视场角，假设角度分辨率为0.1
        sensor.CommonTasks.SetPointingFixedAzEl(satellite_data["yaw"], 0, 0)  # 添加 aboutBoresight 参数，设置为0
        sensorList.append(sensor)

    for sensor in sensorList:
        print(f"传感器 {sensor.InstanceName} 已创建，参数如下:")
        print(f"  Pattern Type: {sensor.PatternType}")



    # 根据配置文件定义覆盖目标
    for target in config_data["targets"]:
        if target['type'] == 'area':
            print(f"目标区域 {target['name']} 已创建，坐标如下:")
            for coordinate in target['coordinates']:
                print(f"  Coordinate: {coordinate}")
            tmp_result = []
            # 创建目标区域
            target_area = root.CurrentScenario.Children.New(AgESTKObjectType.eAreaTarget, target['name'])
            target_area.AreaType = AgEAreaType.ePattern
            patterns = target_area.AreaTypeData
            for coordinate in target['coordinates']:
                patterns.Add(coordinate[0], coordinate[1])

            # 使用CoverageDefinition计算覆盖率
            coverage_definition = root.CurrentScenario.Children.New(AgESTKObjectType.eCoverageDefinition,
                                                                    f"{target['name']}_Coverage")

            for se in sensorList:
                coverage_definition.AssetList.Add(se.Path)

            # coverage_definition.AssetList.Add(constellation.Path)
            # coverage_definition.AssetList.Add(sensor.Path)
            coverage_definition.Grid.BoundsType = 0 # eBoundsCustomRegions
            covGrid = coverage_definition.Grid
            bounds = covGrid.Bounds
            bounds.AreaTargets.Add(target_area.Path)
            coverage_definition.Grid.Resolution.LatLon = 0.5  # 定义网格分辨率

            # 关闭显示网格点
            coverage_definition.Graphics.Static.IsPointsVisible = False

            # 计算覆盖率
            coverage_definition.ComputeAccesses()

            # 提取相关数据
            percent_coverage_data = coverage_definition.DataProviders.Item('Percent Coverage')
            percent_coverage_results = percent_coverage_data.Exec(root.CurrentScenario.StartTime, root.CurrentScenario.StopTime, step_time)
            percent_coverage = percent_coverage_results.DataSets.GetDataSetByName('Percent Coverage').GetValues()[0]

            # 计算最大覆盖间隙持续时间
            gap_definition = coverage_definition.DataProviders.Item('Coverage Gap Duration')
            gap_results = gap_definition.Exec()
            coverage_gaps = gap_results.DataSets.GetDataSetByName('Duration').GetValues()
            max_coverage_gap = max(coverage_gaps) if coverage_gaps else 0
            tmp_result.append("area")
            tmp_result.append(target['name'])
            tmp_result.append(percent_coverage)
            tmp_result.append(max_coverage_gap)
            result.append(tmp_result)
            # print(f"区域 {target['name']} 的覆盖结果:")
            # print(f"覆盖率: {percent_coverage}%")
            # print(f"最大覆盖间隙持续时间: {max_coverage_gap} 秒")

        elif target['type'] == 'point':
            tmp_result = []
            # 创建目标点
            target_point = root.CurrentScenario.Children.New(AgESTKObjectType.eFacility, target['name'])
            target_point.Position.AssignGeodetic(target['coordinates'][0], target['coordinates'][1], 0.0)

            tmp_result.append("point")
            tmp_result.append(target['name'])
            for se in sensorList:
                se_name = se.InstanceName
                access = se.GetAccessToObject(target_point)
                access.ComputeAccess()

                access_data = access.DataProviders.Item('Access Data')
                results = access_data.Exec(root.CurrentScenario.StartTime, root.CurrentScenario.StopTime)

                start_times_list = results.DataSets.GetDataSetByName('Start Time').GetValues()
                stop_times_list = results.DataSets.GetDataSetByName('Stop Time').GetValues()
                durations_list = results.DataSets.GetDataSetByName('Duration').GetValues()

                tilt_angles = []
                side_lobe_angles = []
                side_lobe_angles_e = []
                for i in range(len(start_times_list)):
                    start_time = start_times_list[i]
                    stop_time = stop_times_list[i]

                    # 调用 Attitude_response_control 中的 calculate_satellite_geodetic 和 calculate_side_lobe_angle 函数
                    sat_lon, sat_lat, sat_alt = calculate_satellite_geodetic(
                        satellite_data['perigeeAltitude'],
                        satellite_data['apogeeAltitude'],
                        satellite_data['inclination'],
                        satellite_data['ascNode'],
                        satellite_data['argOfPerigee'],
                        satellite_data['trueAnomaly'],
                        start_time
                    )

                    # target_position = (target['coordinates'][0], target['coordinates'][1], 0.0)
                    side_lobe_angle = calculate_side_lobe_angle(sat_lon, sat_lat, sat_alt,
                                                                target['coordinates'][0],
                                                                target['coordinates'][1],
                                                                0.0)
                    side_lobe_angles.append(side_lobe_angle)

                    sat_lon_e, sat_la_e, sat_alt_e = calculate_satellite_geodetic(
                        satellite_data['perigeeAltitude'],
                        satellite_data['apogeeAltitude'],
                        satellite_data['inclination'],
                        satellite_data['ascNode'],
                        satellite_data['argOfPerigee'],
                        satellite_data['trueAnomaly'],
                        stop_time
                    )
                    side_lobe_angle_e = calculate_side_lobe_angle(sat_lon_e, sat_alt_e, sat_alt_e,
                                                                target['coordinates'][0],
                                                                target['coordinates'][1],
                                                                0.0)
                    side_lobe_angles_e.append(side_lobe_angle_e)
                tmp_sate = []
                tmp_sate.append(se_name)
                tmp_sate.append(start_times_list)
                tmp_sate.append(stop_times_list)
                tmp_sate.append(durations_list)
                tmp_sate.append(side_lobe_angles)
                tmp_sate.append(side_lobe_angles_e)
                tmp_result.append(tmp_sate)
            result.append(tmp_result)
                # print(f"点 {target['name']} 的覆盖结果，卫星 {satellite.InstanceName}:")
                # print(f"开始时间: {start_times}")
                # print(f"结束时间: {stop_times}")
                # print(f"持续时间: {durations}")

        elif target['type'] == 'moving_point':
            tmp_result = []
            # 创建移动目标点
            target_moving_point = root.CurrentScenario.Children.New(AgESTKObjectType.eFacility, target['name'])

            # 存储所有覆盖时间段
            all_coverage_intervals = {se.InstanceName: [] for se in sensorList}

            # 定义移动路径
            for i in range(len(target['path']) - 1):
                start_path_point = target['path'][i]
                end_path_point = target['path'][i + 1]
                start_time_str = start_path_point['time']
                end_time_str = end_path_point['time']
                coordinates = start_path_point['coordinates']
                target_moving_point.Position.AssignGeodetic(coordinates[0], coordinates[1], 0.0)

                # 使用Access计算覆盖率
                for se in sensorList:
                    access = se.GetAccessToObject(target_moving_point)
                    try:
                        access.ComputeAccess()
                        # 提取相关数据
                        access_data = access.DataProviders.Item('Access Data')



                        results = access_data.Exec(start_time_str, end_time_str)

                        start_times = results.DataSets.GetDataSetByName('Start Time').GetValues()
                        stop_times = results.DataSets.GetDataSetByName('Stop Time').GetValues()

                        # 添加到所有覆盖时间段中
                        all_coverage_intervals[se.InstanceName].extend(zip(start_times, stop_times))
                    except Exception as e:
                        # print(f"计算移动点 {target['name']} 在时间 {start_time_str} 到 {end_time_str} 的覆盖时发生错误，卫星 {satellite.InstanceName}: {e}")
                        continue
            # 检查最后一个时间段是否需要延续到场景结束时间
            last_path_point = target['path'][-1]
            last_time_str = last_path_point['time']
            last_coordinates = last_path_point['coordinates']
            if parse_datetime_with_ms(last_time_str) < parse_datetime_with_ms(stop_time_str):
                target_moving_point.Position.AssignGeodetic(last_coordinates[0], last_coordinates[1], 0.0)
                for se in sensorList:
                    access = se.GetAccessToObject(target_moving_point)
                    try:
                        access.ComputeAccess()
                        access_data = access.DataProviders.Item('Access Data')
                        results = access_data.Exec(last_time_str, stop_time_str)

                        start_times = results.DataSets.GetDataSetByName('Start Time').GetValues()
                        stop_times = results.DataSets.GetDataSetByName('Stop Time').GetValues()

                        # 添加到所有覆盖时间段中
                        all_coverage_intervals[se.InstanceName].extend(zip(start_times, stop_times))
                    except Exception as e:
                        # print(f"计算移动点 {target['name']} 在时间 {last_time_str} 到 {stop_time_str} 的覆盖时发生错误，卫星 {satellite.InstanceName}: {e}")
                        continue
            # 合并所有覆盖时间段并输出结果
            tmp_result.append("moving_point")
            tmp_result.append(target['name'])
            for se_name, intervals in all_coverage_intervals.items():
                tmp_sate = []
                start_times_list = []
                stop_times_list = []
                durations_list = []
                side_lobe_angles = []
                side_lobe_angles_e = []
                merged_intervals = merge_intervals(intervals)

                if merged_intervals:
                    for interval in merged_intervals:
                        start_time, stop_time = interval
                        duration = (parse_datetime_with_ms(stop_time) - parse_datetime_with_ms(
                            start_time)).total_seconds()
                        start_times_list.append(start_time)
                        stop_times_list.append(stop_time)
                        durations_list.append(duration)

                    satellite_data = config_data["satellites"][0]
                    for start_time in start_times_list:
                        sat_lon, sat_lat, sat_alt = calculate_satellite_geodetic(
                            satellite_data['perigeeAltitude'],
                            satellite_data['apogeeAltitude'],
                            satellite_data['inclination'],
                            satellite_data['ascNode'],
                            satellite_data['argOfPerigee'],
                            satellite_data['trueAnomaly'],
                            start_time
                        )
                        target_lon, target_lat, target_alt = target['path'][0]['coordinates'][1], target['path'][0]['coordinates'][0], 0
                        side_lobe_angle = calculate_side_lobe_angle(sat_lon, sat_lat, sat_alt, target_lon, target_lat,
                                                                    target_alt)
                        side_lobe_angles.append(side_lobe_angle)

                    for stop_time in stop_times_list:
                        sat_lon_e, sat_lat_e, sat_alt_e = calculate_satellite_geodetic(
                            satellite_data['perigeeAltitude'],
                            satellite_data['apogeeAltitude'],
                            satellite_data['inclination'],
                            satellite_data['ascNode'],
                            satellite_data['argOfPerigee'],
                            satellite_data['trueAnomaly'],
                            stop_time
                        )
                        target_lon_e, target_lat_e, target_alt_e = target['path'][0]['coordinates'][1], target['path'][0]['coordinates'][0], 0
                        side_lobe_angle_e = calculate_side_lobe_angle(sat_lon_e, sat_lat_e, sat_alt_e, target_lon_e, target_lat_e,
                                                                    target_alt_e)
                        side_lobe_angles_e.append(side_lobe_angle_e)



                    tmp_sate.append(se_name)
                    tmp_sate.append(start_times_list)
                    tmp_sate.append(stop_times_list)
                    tmp_sate.append(durations_list)
                    tmp_sate.append(side_lobe_angles)
                    tmp_sate.append(side_lobe_angles_e)
                    tmp_result.append(tmp_sate)
                else:
                    # tmp_sate.append(satellite_name)
                    # tmp_sate.append([])
                    # tmp_sate.append([])
                    # tmp_sate.append([])
                    # tmp_result.append(tmp_sate)
                    continue
            result.append(tmp_result)

    # 关闭场景和STK应用程序
    root.CloseScenario()
    stk.ShutDown()



    python_end_time = time.time()
    execution_time = python_end_time - python_start_time
    print(f"代码执行时间：{execution_time} 秒")
    return result




if __name__ == "__main__":
    config_path = "moving_config.json"  # 设置配置文件路径
    scenario_name = "530MyNewSc"  # 场景名称
    start_time_str = "2 Jun 2024 04:00:00.000"  #  覆盖分析开始时间
    stop_time_str = "3 Jun 2024 04:00:00.000"  # 覆盖分析结束时间
    step_time = 60  # 覆盖分析步长时间（秒）

    print(run_coverage_analysis(config_path, scenario_name, start_time_str, stop_time_str, step_time))
