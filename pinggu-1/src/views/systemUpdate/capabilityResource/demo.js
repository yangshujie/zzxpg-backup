// 测站
{
    "name": "某测控数传站",
    "subSystemType": 1,
    "status": 0,
    "description": "用于卫星测控与数传任务的地面测站设备",
    "side": 1,
    "equipmentType": 9,
    "basicInfo": {
      "stationName": "喀什测控站",
      "longitude": 75.989000,
      "latitude": 39.467000,
      "altitude": 1285.50,
      "stationCode": "KASHI_666"
    },
    "parameters": {
      "workMode": 1,
      "maxTrackingRadius": 12000.000,
      "maxLineSightRate": 0.8500,
      "maxDistance": 15000.000,
      "antennaInfos": [
        {
            "frequencyDetected": [
                {
                "frequencyMin": 1.0,   // GHz
                "frequencyMax": 10.0   // GHz
                },
                {
                "frequencyMin": 10.0,  // GHz
                "frequencyMax": 30.0   // GHz
                }
                // ...
            ],
            "minElevationAngle": 0.0,   // deg
            "minReceiveAngle": 1.0      // deg
        }
        // ...
      ]
    }
}
// 船只
{
    "name": "某船只",
    "subSystemType": 1,
    "status": 0,
    "description": "船只",
    "side": 1,
    "equipmentType": 12,
    "basicInfo": {
      "code": "DDG-173",
      "country": "China",
      "shipType": 0,
      "displacement": 7500.00
    },
    "parameters": {
    "heading": 135.50,
    "speed": 28.75
    }
  }
// 卫星
  {
    "name": "航天侦察卫星A",
    "subSystemType": 0,
    "status": 0,
    "description": "用于光学、SAR、电子侦察的一体化侦察卫星",
    "equipmentType": 5,
    "side": 1,
    "basicInfo": {
        "country": "CN",
        "operator": "某侦察中心",
        "orbitType": "0",
        "orbitAltitude": 5E+2,
        "mass": 4.2E+3,
        "designLife": 8,
        "tleLine1": "1 99999U 20001A   20001.00000000  .00000000  00000-0  00000-0 0 00001",
        "tleLine2": "2 99999  97.0000 0.0000 0000001 0.0000 360.0000 15.00000000 00001",
        "satName": "航天侦察卫星C",
        "semiMajorAxis": 6878.137,//半长轴 km
        "eccentricity": 1E-7,//偏心率
        "inclination": 97,//轨道倾角 °
        "raan": 0,//升交点赤经 °
        "argPerigee": 0,//近地点幅角 °
        "meanAnomaly": 3.6E+2,//平近点角
        "norad": "99999",
        "orbitEpoch": 1234567890123
    },
    "parameters": [
        "123",
        "456"
    ]
}