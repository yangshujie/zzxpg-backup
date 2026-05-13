{
    "sampleCount": 20,
    "blueSamplingType": "简单随机",
    "blueSampleCount": 2,
    "redSamplingType": "均匀分层",
    "redSampleCount": 10,
    "geoConditionList": [
        {
            "conditionId": 156,
            "profileId": 91,
            "envType": "地理环境",
            "geoSubType": ["森林","海洋","高山"],
            "samplingType": "简单随机",
            "polygonCoords": [[1.0,2.0,2.0],[1.0,22.0,3.0],[1.0,2.0,1.0]]
        },
        {
            "conditionId": 157,
            "profileId": 91,
            "envType": "地理环境",
            "geoSubType": ["森林","海洋"],
            "samplingType": "简单随机",
            "polygonCoords": [[1.0,2.0,2.0],[1.0,22.0,3.0],[1.0,2.0,1.0]]
        }
    ],
    "weatherConditionList": [
        {
            "conditionId": 100,
            "profileId": 91,
            "envType": "气象海洋",
            "weatherType": ["云","雨"],
            "samplingType": "简单随机",
            "subSamplingType": {
                "云": { "level": [1,2,3,4,5], "samplingType": "拉丁超立方" },
                "雨": { "level": [1,2,3,4,5], "samplingType": "拉丁超立方" },
                "雾": { "level": [1,2,3,4,5], "samplingType": "拉丁超立方" }
            },
            "weatherLevel": [],
            "validStartTime": "2026-04-07T00:00:00",
            "validEndTime": "2026-04-08T00:00:00",
            "polygonCoords": [[137.0,40.0,0.0],[138.0,41.0,0.0]]
        }
    ],
    "electromagneticConditionList": [
        {
            "conditionId": 75,
            "profileId": 91,
            "envType": "空间电磁",
            "geoSubType": ["太阳耀斑"],
            "solarFlareLevel": [],
            "validStartTime": "2026-04-07T00:00:00",
            "validEndTime": "2026-04-08T00:00:00",
            "polygonCoords": []
        }
    ],
    "blueEquipmentList": [
        {
            "equipmentId": "5F966D5E-4DED-4D94-AAE8-02C4D8D2963D",
            "profileId": 91,
            "equipmentName": "东区",
            "subSystemType": 0,
            "equipmentLevel": 1,
            "equipmentType": 13,
            "id": 53
        },
        {
            "equipmentId": "05C9D122-2595-43C7-80C3-5D65FF174007",
            "profileId": 91,
            "equipmentName": "地面处理中心",
            "subSystemType": 0,
            "equipmentLevel": 1,
            "equipmentType": 13,
            "id": 54
        }
    ],
    "redEquipmentList": [
            {
                "equipmentId": "AD65BAE7-C230-498B-BD41-13C5F13CBA27",
                "profileId": 91,
                "equipmentName": "XX1705",
                "subSystemType": 0,
                "equipmentLevel": 1,
                "equipmentType": 0,
                "id": 839
            },
            {
                "equipmentId": "49CFA867-FFBF-427F-8F11-9BA841E2AD16",
                "profileId": 91,
                "equipmentName": "XX1203",
                "subSystemType": 0,
                "equipmentLevel": 1,
                "equipmentType": 0,
                "id": 840
            },
            {
                "equipmentId": "AC3D2E34-BD9A-4E24-9F22-2886BD8CFD9F",
                "profileId": 91,
                "equipmentName": "XX14A",
                "subSystemType": 0,
                "equipmentLevel": 1,
                "equipmentType": 0,
                "id": 841
            },
            {
                "equipmentId": "5C14BC03-4194-402B-A580-236EEBA73022",
                "profileId": 91,
                "equipmentName": "XX1205",
                "subSystemType": 0,
                "equipmentLevel": 1,
                "equipmentType": 0,
                "id": 842
            },
            {
                "equipmentId": "E2F93840-7291-4B1F-BCCB-AE7B63CDB6F3",
                "profileId": 91,
                "equipmentName": "XX14B",
                "subSystemType": 0,
                "equipmentLevel": 1,
                "equipmentType": 0,
                "id": 843
            },
            {
                "equipmentId": "FE35A1D4-921A-4391-BE37-BFEDB7F1F12C",
                "profileId": 91,
                "equipmentName": "XX1504",
                "subSystemType": 0,
                "equipmentLevel": 1,
                "equipmentType": 5,
                "id": 844
            },
            {
                "equipmentId": "76D625D0-D06E-4C4A-8C7B-AE311DE69283",
                "profileId": 91,
                "equipmentName": "XX1503",
                "subSystemType": 0,
                "equipmentLevel": 1,
                "equipmentType": 5,
                "id": 845
            },
            {
                "equipmentId": "6CBAD91F-A3DC-4FAC-A46D-28396371C830",
                "profileId": 91,
                "equipmentName": "中继测控站",
                "subSystemType": 0,
                "equipmentLevel": 1,
                "equipmentType": 9,
                "id": 846
            },
            {
                "equipmentId": "D837F5C4-8E3F-43D5-BD3D-4F56A4E3FB8D",
                "profileId": 91,
                "equipmentName": "南宁测控站",
                "subSystemType": 0,
                "equipmentLevel": 1,
                "equipmentType": 9,
                "id": 847
            },
            {
                "equipmentId": "6FF5F19F-249F-4854-83AD-D0BEA3ECC5FE",
                "profileId": 91,
                "equipmentName": "陵水测控站",
                "subSystemType": 0,
                "equipmentLevel": 1,
                "equipmentType": 9,
                "id": 848
            },
            {
                "equipmentId": "5FF78195-E9F6-456B-97F0-65CC40D620CF",
                "profileId": 91,
                "equipmentName": "日喀则测控站",
                "subSystemType": 0,
                "equipmentLevel": 1,
                "equipmentType": 9,
                "id": 849
            },
            {
                "equipmentId": "6E1D8E6C-3938-4621-918E-3CFB83D64457",
                "profileId": 91,
                "equipmentName": "勐海测控站",
                "subSystemType": 0,
                "equipmentLevel": 1,
                "equipmentType": 9,
                "id": 850
            },
            {
                "equipmentId": "AA660C1F-2830-42D3-A01E-25EE51D14D6A",
                "profileId": 91,
                "equipmentName": "渭南测控站",
                "subSystemType": 0,
                "equipmentLevel": 1,
                "equipmentType": 9,
                "id": 851
            },
            {
                "equipmentId": "620BB7A9-A9BE-4A74-A74B-C94DDDA264FD",
                "profileId": 91,
                "equipmentName": "青岛测控站",
                "subSystemType": 0,
                "equipmentLevel": 1,
                "equipmentType": 9,
                "id": 852
            },
            {
                "equipmentId": "C7E67731-B8AB-4BDD-AA42-F2E5FF852BE8",
                "profileId": 91,
                "equipmentName": "北京测控站",
                "subSystemType": 0,
                "equipmentLevel": 1,
                "equipmentType": 9,
                "id": 853
            },
            {
                "equipmentId": "B5CE97F2-6B75-4204-B649-1C9863A0855E",
                "profileId": 91,
                "equipmentName": "牡丹江测控站",
                "subSystemType": 0,
                "equipmentLevel": 1,
                "equipmentType": 9,
                "id": 854
            },
            {
                "equipmentId": "A6B87A6A-D499-4150-BCEB-391FEDA4FF58",
                "profileId": 91,
                "equipmentName": "卡拉奇测控站",
                "subSystemType": 0,
                "equipmentLevel": 1,
                "equipmentType": 9,
                "id": 855
            },
            {
                "equipmentId": "A257B519-C54B-4744-B0ED-41172D86B75B",
                "profileId": 91,
                "equipmentName": "美济测控站",
                "subSystemType": 0,
                "equipmentLevel": 1,
                "equipmentType": 9,
                "id": 856
            },
            {
                "equipmentId": "19C4A858-6E95-4A6C-BE33-592465E71BCD",
                "profileId": 91,
                "equipmentName": "昆明测控站",
                "subSystemType": 0,
                "equipmentLevel": 1,
                "equipmentType": 9,
                "id": 857
            },
            {
                "equipmentId": "D250A755-B5D9-4423-99D8-10529214C551",
                "profileId": 91,
                "equipmentName": "三亚测控站",
                "subSystemType": 0,
                "equipmentLevel": 1,
                "equipmentType": 9,
                "id": 858
            },
            {
                "equipmentId": "B2F53CDB-9E46-428D-807A-A97A41DE24E5",
                "profileId": 91,
                "equipmentName": "厦门测控站",
                "subSystemType": 0,
                "equipmentLevel": 1,
                "equipmentType": 9,
                "id": 859
            },
            {
                "equipmentId": "512CA66B-F50C-47B3-B40C-46F3C570CD14",
                "profileId": 91,
                "equipmentName": "阿勒泰测控站",
                "subSystemType": 0,
                "equipmentLevel": 1,
                "equipmentType": 9,
                "id": 860
            },
            {
                "equipmentId": "E2B186F4-5E7C-4290-805C-F7BF38942B06",
                "profileId": 91,
                "equipmentName": "天津测控站",
                "subSystemType": 0,
                "equipmentLevel": 1,
                "equipmentType": 9,
                "id": 861
            },
            {
                "equipmentId": "75ABF166-51E5-4861-9352-2263BED7DCFE",
                "profileId": 91,
                "equipmentName": "佳木斯测控站",
                "subSystemType": 0,
                "equipmentLevel": 1,
                "equipmentType": 9,
                "id": 862
            },
            {
                "equipmentId": "4FD59A15-4352-4638-A570-6AC998894AC2",
                "profileId": 91,
                "equipmentName": "喀什测控站",
                "subSystemType": 0,
                "equipmentLevel": 1,
                "equipmentType": 9,
                "id": 863
            },
            {
                "equipmentId": "18EA054F-6013-40E4-9A60-469785BBA16A",
                "profileId": 91,
                "equipmentName": "西安测控站",
                "subSystemType": 0,
                "equipmentLevel": 1,
                "equipmentType": 9,
                "id": 864
            },
            {
                "equipmentId": "BC0FC56E-EEFE-4F89-A5B0-1583D724C140",
                "profileId": 91,
                "equipmentName": "TL0203",
                "subSystemType": 0,
                "equipmentLevel": 1,
                "equipmentType": 6,
                "id": 865
            },
            {
                "equipmentId": "55EB9A00-8ADE-4339-852E-29C7127F3831",
                "profileId": 91,
                "equipmentName": "TL0105",
                "subSystemType": 0,
                "equipmentLevel": 1,
                "equipmentType": 6,
                "id": 866
            },
            {
                "equipmentId": "536B546E-ABAD-49D3-A549-7A35064BD82B",
                "profileId": 91,
                "equipmentName": "XX1505",
                "subSystemType": 0,
                "equipmentLevel": 1,
                "equipmentType": 5,
                "id": 867
            },
            {
                "equipmentId": "4FA0AA46-EA0B-4539-9174-2AE94021A151",
                "profileId": 91,
                "equipmentName": "XX1504",
                "subSystemType": 0,
                "equipmentLevel": 1,
                "equipmentType": 5,
                "id": 868
            },
            {
                "equipmentId": "6923725E-3C60-44C0-832E-C12C54AFADCF",
                "profileId": 91,
                "equipmentName": "XX1503",
                "subSystemType": 0,
                "equipmentLevel": 1,
                "equipmentType": 5,
                "id": 869
            },
            {
                "equipmentId": "61C7CE75-2B27-4173-9EB5-9224EC12BE4E",
                "profileId": 91,
                "equipmentName": "XX1502",
                "subSystemType": 0,
                "equipmentLevel": 1,
                "equipmentType": 5,
                "id": 870
            },
            {
                "equipmentId": "5DDE4623-E7D9-479A-9F52-A7039B9B77D6",
                "profileId": 91,
                "equipmentName": "XX1705",
                "subSystemType": 0,
                "equipmentLevel": 1,
                "equipmentType": 4,
                "id": 871
            },
            {
                "equipmentId": "E49FCBCF-A6C5-4FC1-B91D-D8586651EAD1",
                "profileId": 91,
                "equipmentName": "XX1704",
                "subSystemType": 0,
                "equipmentLevel": 1,
                "equipmentType": 4,
                "id": 872
            },
            {
                "equipmentId": "9317990B-DF38-47BF-ADB2-D2C6F5A53858",
                "profileId": 91,
                "equipmentName": "XX1703",
                "subSystemType": 0,
                "equipmentLevel": 1,
                "equipmentType": 4,
                "id": 873
            },
            {
                "equipmentId": "624964A1-BC64-4EA0-A985-ADF50B3CBBFA",
                "profileId": 91,
                "equipmentName": "XX1702",
                "subSystemType": 0,
                "equipmentLevel": 1,
                "equipmentType": 4,
                "id": 874
            },
            {
                "equipmentId": "C9D6B34A-5643-423D-8F41-DB844F7E938B",
                "profileId": 91,
                "equipmentName": "XX1701",
                "subSystemType": 0,
                "equipmentLevel": 1,
                "equipmentType": 4,
                "id": 875
            },
            {
                "equipmentId": "E505BDA8-F945-4350-B311-F44D5FE17E02",
                "profileId": 91,
                "equipmentName": "XX13B",
                "subSystemType": 0,
                "equipmentLevel": 1,
                "equipmentType": 4,
                "id": 876
            },
            {
                "equipmentId": "CA6B711C-FE25-4B56-91A6-4F149A94E0EC",
                "profileId": 91,
                "equipmentName": "XX13A",
                "subSystemType": 0,
                "equipmentLevel": 1,
                "equipmentType": 4,
                "id": 877
            },
            {
                "equipmentId": "D89B3D70-5DFA-4BCD-8634-B0BD09C40C04",
                "profileId": 91,
                "equipmentName": "XX1904",
                "subSystemType": 0,
                "equipmentLevel": 1,
                "equipmentType": 2,
                "id": 878
            },
            {
                "equipmentId": "7B110285-78F8-4504-8935-7926135B5682",
                "profileId": 91,
                "equipmentName": "XX1903",
                "subSystemType": 0,
                "equipmentLevel": 1,
                "equipmentType": 2,
                "id": 879
            },
            {
                "equipmentId": "1E5B3DA1-0C80-4A96-91C7-BD39F784E73B",
                "profileId": 91,
                "equipmentName": "XX1902",
                "subSystemType": 0,
                "equipmentLevel": 1,
                "equipmentType": 2,
                "id": 880
            },
            {
                "equipmentId": "8097A789-A747-4C75-88C0-A3A01745A53A",
                "profileId": 91,
                "equipmentName": "XX1901",
                "subSystemType": 0,
                "equipmentLevel": 1,
                "equipmentType": 2,
                "id": 881
            },
            {
                "equipmentId": "288FB25A-CDA5-473D-AEF1-074732AB42D5",
                "profileId": 91,
                "equipmentName": "XX1605",
                "subSystemType": 0,
                "equipmentLevel": 1,
                "equipmentType": 1,
                "id": 882
            },
            {
                "equipmentId": "930EDCD0-2357-49A5-8DE7-C6BCFC7F0023",
                "profileId": 91,
                "equipmentName": "XX1604",
                "subSystemType": 0,
                "equipmentLevel": 1,
                "equipmentType": 1,
                "id": 883
            },
            {
                "equipmentId": "04EB82EF-EB3E-470C-967B-4CA4912941B4",
                "profileId": 91,
                "equipmentName": "XX1603",
                "subSystemType": 0,
                "equipmentLevel": 1,
                "equipmentType": 1,
                "id": 884
            },
            {
                "equipmentId": "307BF1DC-68B6-4711-AE0D-91F52D7DD6D6",
                "profileId": 91,
                "equipmentName": "XX1602",
                "subSystemType": 0,
                "equipmentLevel": 1,
                "equipmentType": 1,
                "id": 885
            },
            {
                "equipmentId": "96FB4BC8-EB74-4F02-9DDD-510675B139DA",
                "profileId": 91,
                "equipmentName": "XX1601",
                "subSystemType": 0,
                "equipmentLevel": 1,
                "equipmentType": 1,
                "id": 886
            }]
}