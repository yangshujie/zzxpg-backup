{
   
    "basicInfo": {
        "profileName": "打击剖面",
        "profileTypeId": "航天装备侦察子体系",
        "operationTarget": "蓝方",
        "operationIntensity": 0.5,
        "operationRequirement": "天基侦察目指保障",
        "blueMaxRadius": 500,
        "blueRadiusUnit": "公里",
        "blueReactionTime": 15,
        "blueReactionUnit": "分钟",
        "blueSustainTime": 72,
        "blueSustainUnit": "小时",
        "blueStrategy":  ["电磁静默", "角反射器干扰"]
    },
    "taskList": [
        {
            "taskName": "侦察阶段",
            "taskStage": "1"
           
        },
        {
            "taskName": "指挥决策",
            "taskStage": "2"
        },
        {
            "taskName": "火力打击",
            "taskStage": "3"
        },
        {
            "taskName": "效果评估",
            "taskStage": "4"
        }
    ],
    "geoConditionList": [
    {
        "envType": "地理环境",
        "geoSubType": ["高山", "森林"],
        "polygonCoords": [[112.3456,20.1234,0],[113.3456,21.1234,0]]
    }
    ],
    "weatherConditionList": [
            {
      "envType": "气象海洋",
      "validStartTime": "2026-03-01T08:00:00",
      "validEndTime": "2026-03-01T18:00:00",
      "polygonCoords": [[112.3456,20.1234,0],[113.3456,21.1234,0]],
      "weatherType": ["雨", "云", "雾"],
      "weatherLevel": []
    },
    {
      "envType": "气象海洋",
      "validStartTime": "2026-03-02T08:00:00",
      "validEndTime": "2026-03-02T18:00:00",
      "polygonCoords": [[112.111,20.111,0],[113.111,21.111,0]],
      "weatherType": ["云"],
      "weatherLevel": []
    }
    ],
    "electromagneticConditionList": [
        {
            "envType": "空间电磁",
            "geoSubType": ["太阳耀斑"],
            "solar_flare_level": [],
            "validStartTime": "2026-03-01T08:00:00",
            "validEndTime": "2026-03-01T18:00:00",
            "polygonCoords": [[112.5678901234,20.5678901234,0],[113.5678901234,21.5678901234,0]]
        }
    ],
    "blueEquipmentList": [
        {   "equipmentId": 1,
            "equipmentName": "福特号航母战斗群",
            "subSystemType": 0,
            "equipmentLevel": 0,
            "equipmentType": 0
        }
    ],
    "redEquipmentList": [
        {   "equipmentId": 2,
            "equipmentName": "电子卫星X1",
            "subSystemType": 1,
            "equipmentLevel": 1,
            "equipmentType": 1
        }
    ]
}