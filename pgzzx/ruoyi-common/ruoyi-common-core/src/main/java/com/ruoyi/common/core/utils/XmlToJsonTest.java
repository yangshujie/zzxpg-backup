package com.ruoyi.common.core.utils;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.XML;
import com.ruoyi.common.core.utils.jsonprase.JsonUtils;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class XmlToJsonTest
{


    public static void main(String[] args)
    {

        String xml =
                "<itemsPreviousRespondHeader>"
                        +      "<language>zh-cn</language>"
                        +     "<respondTime name=\"aaa\">2010-04-28T18:36:00</respondTime>"
                        +      "<respondStatus>"
                        +           "<respondCode>0000</respondCode>"
                        +          "<respondInfo>整批交易全部成功！</respondInfo>"
                        +     "</respondStatus>"
                        +      "<userID></userID>"
                        +     "<batchID>205027899520091023600000</batchID>"
                        +     "<transPatches>1</transPatches>"
                        +   "</itemsPreviousRespondHeader>";


        JSONObject object = XML.toJSONObject(xml);

        String str_object = object.toString();

        Map<String, Object> jsonMap=JsonUtils.transferToMap(str_object);

        System.out.println(jsonMap);
//
//        String e = JsonUtils.getValue(str_object, "itemsPreviousRespondHeader.respondStatus.respondCode", String.class);
//        System.out.println(e);
//        System.out.println(jsonMap);
    }

    @Test
    public void ea() {

        String xml = "((((((((**(((((((((((((())))))))))))))))";

        xml=xml.replace("(","");
        System.out.println(xml);
    }

    @Test
    public void kkkk()
    {

        String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">\n" +
                "    <soap:Body>\n" +
                "        <QueryFAUDS_RESULTResponse xmlns=\"http://tempuri.org/\">\n" +
                "            <QueryFAUDS_RESULTResult>\n" +
                "                <xs:schema id=\"NewDataSet\" xmlns=\"\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:msdata=\"urn:schemas-microsoft-com:xml-msdata\">\n" +
                "                    <xs:element name=\"NewDataSet\" msdata:IsDataSet=\"true\" msdata:UseCurrentLocale=\"true\">\n" +
                "                        <xs:complexType>\n" +
                "                            <xs:choice minOccurs=\"0\" maxOccurs=\"unbounded\">\n" +
                "                                <xs:element name=\"Table\">\n" +
                "                                    <xs:complexType>\n" +
                "                                        <xs:sequence>\n" +
                "                                            <xs:element name=\"I\" type=\"xs:decimal\" minOccurs=\"0\" />\n" +
                "                                            <xs:element name=\"FT\" type=\"xs:dateTime\" minOccurs=\"0\" />\n" +
                "                                            <xs:element name=\"DT\" type=\"xs:dateTime\" minOccurs=\"0\" />\n" +
                "                                            <xs:element name=\"PI\" type=\"xs:string\" minOccurs=\"0\" />\n" +
                "                                            <xs:element name=\"DI\" type=\"xs:string\" minOccurs=\"0\" />\n" +
                "                                            <xs:element name=\"R\" type=\"xs:string\" minOccurs=\"0\" />\n" +
                "                                            <xs:element name=\"S\" type=\"xs:string\" minOccurs=\"0\" />\n" +
                "                                            <xs:element name=\"SN\" type=\"xs:string\" minOccurs=\"0\" />\n" +
                "                                            <xs:element name=\"ST\" type=\"xs:decimal\" minOccurs=\"0\" />\n" +
                "                                        </xs:sequence>\n" +
                "                                    </xs:complexType>\n" +
                "                                </xs:element>\n" +
                "                            </xs:choice>\n" +
                "                        </xs:complexType>\n" +
                "                    </xs:element>\n" +
                "                </xs:schema>\n" +
                "                <diffgr:diffgram xmlns:msdata=\"urn:schemas-microsoft-com:xml-msdata\" xmlns:diffgr=\"urn:schemas-microsoft-com:xml-diffgram-v1\">\n" +
                "                    <NewDataSet xmlns=\"\">\n" +
                "                        <Table diffgr:id=\"Table1\" msdata:rowOrder=\"0\">\n" +
                "                            <I>2</I>\n" +
                "                            <FT>2021-02-02T16:20:15+08:00</FT>\n" +
                "                            <DT>2021-02-02T16:20:19+08:00</DT>\n" +
                "                            <DI>信息：航天器标识(SCID)为251!\n" +
                "等级：正常\n" +
                "\t信源：0x3a150f00\n" +
                "\t诊断参数：\n" +
                "\t遥测：scid[航天器标识]，值为：251(fbH)\n" +
                "\t成立条件：\n" +
                "\tscid(=251) == 251 \n" +
                "\n" +
                "\t</DI>\n" +
                "                            <R>Rule_137604_SCID_sub_1</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>0</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table2\" msdata:rowOrder=\"1\">\n" +
                "                            <I>27</I>\n" +
                "                            <FT>2021-01-19T17:32:50+08:00</FT>\n" +
                "                            <DT>2021-01-19T17:33:23+08:00</DT>\n" +
                "                            <DI>信息：KBZ12_天线遮挡太敏标志(RKSB472)跳变为遮挡!\n" +
                "等级：正常\n" +
                "\t信源：0x3a150f00\n" +
                "\t诊断参数：\n" +
                "\t遥测：rksb472[KBZ12_天线遮挡太敏标志]，值为：1(1H)\n" +
                "\t成立条件：\n" +
                "\t rksb472==1\n" +
                "\t</DI>\n" +
                "                            <R>Rule_133068_RKSB472</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>0</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table3\" msdata:rowOrder=\"2\">\n" +
                "                            <I>31</I>\n" +
                "                            <FT>2021-01-22T06:39:49+08:00</FT>\n" +
                "                            <DT>2021-01-22T06:39:53+08:00</DT>\n" +
                "                            <DI>信息：航天器标识(SCID)为251!\n" +
                "等级：正常\n" +
                "\t信源：0x3a150f00\n" +
                "\t诊断参数：\n" +
                "\t遥测：scid[航天器标识]，值为：251(fbH)\n" +
                "\t成立条件：\n" +
                "\tscid(=251) == 251 \n" +
                "\n" +
                "\t</DI>\n" +
                "                            <R>Rule_137604_SCID_sub_1</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>0</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table4\" msdata:rowOrder=\"3\">\n" +
                "                            <I>33</I>\n" +
                "                            <FT>2021-01-18T00:14:19+08:00</FT>\n" +
                "                            <DT>2021-01-18T00:14:55+08:00</DT>\n" +
                "                            <DI>信息：KBZ12_天线遮挡太敏标志(RKSB472)跳变为遮挡!\n" +
                "等级：正常\n" +
                "\t信源：0x3a150f00\n" +
                "\t诊断参数：\n" +
                "\t遥测：rksb472[KBZ12_天线遮挡太敏标志]，值为：1(1H)\n" +
                "\t成立条件：\n" +
                "\t rksb472==1\n" +
                "\t</DI>\n" +
                "                            <R>Rule_133068_RKSB472</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>0</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table5\" msdata:rowOrder=\"4\">\n" +
                "                            <I>39</I>\n" +
                "                            <FT>2021-01-18T00:14:38+08:00</FT>\n" +
                "                            <DT>2021-01-18T00:15:13+08:00</DT>\n" +
                "                            <DI>信息：KBZ12_天线遮挡太敏标志(RKSB472)跳变为遮挡!\n" +
                "等级：正常\n" +
                "\t信源：0x3a150f00\n" +
                "\t诊断参数：\n" +
                "\t遥测：rksb472[KBZ12_天线遮挡太敏标志]，值为：1(1H)\n" +
                "\t成立条件：\n" +
                "\t rksb472==1\n" +
                "\t</DI>\n" +
                "                            <R>Rule_133068_RKSB472</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>0</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table6\" msdata:rowOrder=\"5\">\n" +
                "                            <I>41</I>\n" +
                "                            <FT>2021-01-18T17:32:54+08:00</FT>\n" +
                "                            <DT>2021-01-18T17:33:27+08:00</DT>\n" +
                "                            <DI>信息：KBZ12_天线遮挡太敏标志(RKSB472)跳变为遮挡!\n" +
                "等级：正常\n" +
                "\t信源：0x3a150f00\n" +
                "\t诊断参数：\n" +
                "\t遥测：rksb472[KBZ12_天线遮挡太敏标志]，值为：1(1H)\n" +
                "\t成立条件：\n" +
                "\t rksb472==1\n" +
                "\t</DI>\n" +
                "                            <R>Rule_133068_RKSB472</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>0</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table7\" msdata:rowOrder=\"6\">\n" +
                "                            <I>44</I>\n" +
                "                            <FT>2021-01-19T00:15:48+08:00</FT>\n" +
                "                            <DT>2021-01-19T03:40:24+08:00</DT>\n" +
                "                            <DI>-</DI>\n" +
                "                            <R>Rule_133068_RKSB472</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>1</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table8\" msdata:rowOrder=\"7\">\n" +
                "                            <I>45</I>\n" +
                "                            <FT>2021-01-19T09:24:11+08:00</FT>\n" +
                "                            <DT>2021-01-19T09:24:30+08:00</DT>\n" +
                "                            <DI>信息：SP120VC8选址组合数据包3使能标识(RDSP155)由禁止跳变为使能!\n" +
                "等级：一般\n" +
                "\t信源：0x3a150f00\n" +
                "\t诊断参数：\n" +
                "\t遥测：rdsp155[SP120VC8选址组合数据包3使能标识]，值为：1(1H)\n" +
                "\t成立条件：\n" +
                "\t rdsp155==1\n" +
                "\t</DI>\n" +
                "                            <R>Rule_137762_RDSP155</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>0</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table9\" msdata:rowOrder=\"8\">\n" +
                "                            <I>47</I>\n" +
                "                            <FT>2021-01-19T09:25:25+08:00</FT>\n" +
                "                            <DT>2021-01-19T09:29:42+08:00</DT>\n" +
                "                            <DI>-</DI>\n" +
                "                            <R>Rule_137765_RDSP158</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>1</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table10\" msdata:rowOrder=\"9\">\n" +
                "                            <I>48</I>\n" +
                "                            <FT>2021-01-19T09:24:11+08:00</FT>\n" +
                "                            <DT>2021-01-19T09:29:53+08:00</DT>\n" +
                "                            <DI>-</DI>\n" +
                "                            <R>Rule_137762_RDSP155</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>1</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table11\" msdata:rowOrder=\"10\">\n" +
                "                            <I>49</I>\n" +
                "                            <FT>2021-01-19T17:33:10+08:00</FT>\n" +
                "                            <DT>2021-01-19T17:33:42+08:00</DT>\n" +
                "                            <DI>信息：KBZ12_天线遮挡太敏标志(RKSB472)跳变为遮挡!\n" +
                "等级：正常\n" +
                "\t信源：0x3a150f00\n" +
                "\t诊断参数：\n" +
                "\t遥测：rksb472[KBZ12_天线遮挡太敏标志]，值为：1(1H)\n" +
                "\t成立条件：\n" +
                "\t rksb472==1\n" +
                "\t</DI>\n" +
                "                            <R>Rule_133068_RKSB472</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>0</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table12\" msdata:rowOrder=\"11\">\n" +
                "                            <I>50</I>\n" +
                "                            <FT>2021-01-19T17:33:10+08:00</FT>\n" +
                "                            <DT>2021-01-19T21:24:17+08:00</DT>\n" +
                "                            <DI>-</DI>\n" +
                "                            <R>Rule_133068_RKSB472</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>1</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table13\" msdata:rowOrder=\"12\">\n" +
                "                            <I>52</I>\n" +
                "                            <FT>2021-01-21T23:30:25+08:00</FT>\n" +
                "                            <DT>2021-01-21T23:30:25+08:00</DT>\n" +
                "                            <DI>信息：帧同步码(SYN)正确!\n" +
                "等级：正常\n" +
                "\t信源：0x66030e08\n" +
                "\t诊断参数：\n" +
                "\t遥测：syn[帧同步码]，值为：449838109(1acffc1dH)\n" +
                "\t成立条件：\n" +
                "\t syn==449838109\n" +
                "\t</DI>\n" +
                "                            <R>Rule_137605_SYN_sub_1</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>0</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table14\" msdata:rowOrder=\"13\">\n" +
                "                            <I>53</I>\n" +
                "                            <FT>2021-01-18T00:04:25+08:00</FT>\n" +
                "                            <DT>2021-01-18T00:04:29+08:00</DT>\n" +
                "                            <DI>信息：航天器标识(SCID)为251!\n" +
                "等级：正常\n" +
                "\t信源：0x3a150f00\n" +
                "\t诊断参数：\n" +
                "\t遥测：scid[航天器标识]，值为：251(fbH)\n" +
                "\t成立条件：\n" +
                "\tscid(=251) == 251 \n" +
                "\n" +
                "\t</DI>\n" +
                "                            <R>Rule_137604_SCID_sub_1</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>0</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table15\" msdata:rowOrder=\"14\">\n" +
                "                            <I>61</I>\n" +
                "                            <FT>2021-01-19T09:28:18+08:00</FT>\n" +
                "                            <DT>2021-01-19T09:28:36+08:00</DT>\n" +
                "                            <DI>信息：SP120VC8选址组合数据包3使能标识(RDSP155)由禁止跳变为使能!\n" +
                "等级：一般\n" +
                "\t信源：0x3a150f00\n" +
                "\t诊断参数：\n" +
                "\t遥测：rdsp155[SP120VC8选址组合数据包3使能标识]，值为：1(1H)\n" +
                "\t成立条件：\n" +
                "\t rdsp155==1\n" +
                "\t</DI>\n" +
                "                            <R>Rule_137762_RDSP155</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>0</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table16\" msdata:rowOrder=\"15\">\n" +
                "                            <I>64</I>\n" +
                "                            <FT>2021-01-19T09:28:18+08:00</FT>\n" +
                "                            <DT>2021-01-19T09:33:59+08:00</DT>\n" +
                "                            <DI>-</DI>\n" +
                "                            <R>Rule_137762_RDSP155</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>1</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table17\" msdata:rowOrder=\"16\">\n" +
                "                            <I>67</I>\n" +
                "                            <FT>2021-01-20T23:41:49+08:00</FT>\n" +
                "                            <DT>2021-01-20T23:41:52+08:00</DT>\n" +
                "                            <DI>信息：帧同步码(SYN)正确!\n" +
                "等级：正常\n" +
                "\t信源：0x3a150f00\n" +
                "\t诊断参数：\n" +
                "\t遥测：syn[帧同步码]，值为：449838109(1acffc1dH)\n" +
                "\t成立条件：\n" +
                "\t syn==449838109\n" +
                "\t</DI>\n" +
                "                            <R>Rule_137605_SYN_sub_1</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>0</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table18\" msdata:rowOrder=\"17\">\n" +
                "                            <I>70</I>\n" +
                "                            <FT>2021-01-20T23:56:08+08:00</FT>\n" +
                "                            <DT>2021-01-21T03:21:37+08:00</DT>\n" +
                "                            <DI>-</DI>\n" +
                "                            <R>Rule_133068_RKSB472</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>1</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table19\" msdata:rowOrder=\"18\">\n" +
                "                            <I>72</I>\n" +
                "                            <FT>2021-01-21T17:14:25+08:00</FT>\n" +
                "                            <DT>2021-01-21T21:06:10+08:00</DT>\n" +
                "                            <DI>-</DI>\n" +
                "                            <R>Rule_133068_RKSB472</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>1</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table20\" msdata:rowOrder=\"19\">\n" +
                "                            <I>74</I>\n" +
                "                            <FT>2021-01-21T23:57:18+08:00</FT>\n" +
                "                            <DT>2021-01-22T03:21:54+08:00</DT>\n" +
                "                            <DI>-</DI>\n" +
                "                            <R>Rule_133068_RKSB472</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>1</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table21\" msdata:rowOrder=\"20\">\n" +
                "                            <I>81</I>\n" +
                "                            <FT>2021-01-24T23:11:55+08:00</FT>\n" +
                "                            <DT>2021-01-24T23:11:56+08:00</DT>\n" +
                "                            <DI>信息：帧同步码(SYN)正确!\n" +
                "等级：正常\n" +
                "\t信源：0x66030e08\n" +
                "\t诊断参数：\n" +
                "\t遥测：syn[帧同步码]，值为：449838109(1acffc1dH)\n" +
                "\t成立条件：\n" +
                "\t syn==449838109\n" +
                "\t</DI>\n" +
                "                            <R>Rule_137605_SYN_sub_1</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>0</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table22\" msdata:rowOrder=\"21\">\n" +
                "                            <I>82</I>\n" +
                "                            <FT>2021-01-24T23:11:55+08:00</FT>\n" +
                "                            <DT>2021-01-24T23:11:56+08:00</DT>\n" +
                "                            <DI>信息：航天器标识(SCID)为251!\n" +
                "等级：正常\n" +
                "\t信源：0x66030e08\n" +
                "\t诊断参数：\n" +
                "\t遥测：scid[航天器标识]，值为：251(fbH)\n" +
                "\t成立条件：\n" +
                "\tscid(=251) == 251 \n" +
                "\n" +
                "\t</DI>\n" +
                "                            <R>Rule_137604_SCID_sub_1</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>0</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table23\" msdata:rowOrder=\"22\">\n" +
                "                            <I>86</I>\n" +
                "                            <FT>2021-01-18T00:14:30+08:00</FT>\n" +
                "                            <DT>2021-01-18T03:39:58+08:00</DT>\n" +
                "                            <DI>-</DI>\n" +
                "                            <R>Rule_133068_RKSB472</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>1</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table24\" msdata:rowOrder=\"23\">\n" +
                "                            <I>91</I>\n" +
                "                            <FT>2021-01-19T09:24:03+08:00</FT>\n" +
                "                            <DT>2021-01-19T09:24:21+08:00</DT>\n" +
                "                            <DI>信息：SP120VC8选址组合数据包3使能标识(RDSP155)由禁止跳变为使能!\n" +
                "等级：一般\n" +
                "\t信源：0x3a150f00\n" +
                "\t诊断参数：\n" +
                "\t遥测：rdsp155[SP120VC8选址组合数据包3使能标识]，值为：1(1H)\n" +
                "\t成立条件：\n" +
                "\t rdsp155==1\n" +
                "\t</DI>\n" +
                "                            <R>Rule_137762_RDSP155</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>0</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table25\" msdata:rowOrder=\"24\">\n" +
                "                            <I>93</I>\n" +
                "                            <FT>2021-01-19T09:25:16+08:00</FT>\n" +
                "                            <DT>2021-01-19T09:29:33+08:00</DT>\n" +
                "                            <DI>-</DI>\n" +
                "                            <R>Rule_137765_RDSP158</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>1</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table26\" msdata:rowOrder=\"25\">\n" +
                "                            <I>94</I>\n" +
                "                            <FT>2021-01-19T09:24:03+08:00</FT>\n" +
                "                            <DT>2021-01-19T09:29:44+08:00</DT>\n" +
                "                            <DI>-</DI>\n" +
                "                            <R>Rule_137762_RDSP155</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>1</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table27\" msdata:rowOrder=\"26\">\n" +
                "                            <I>97</I>\n" +
                "                            <FT>2021-01-21T23:30:16+08:00</FT>\n" +
                "                            <DT>2021-01-21T23:30:17+08:00</DT>\n" +
                "                            <DI>信息：帧同步码(SYN)正确!\n" +
                "等级：正常\n" +
                "\t信源：0x66030e08\n" +
                "\t诊断参数：\n" +
                "\t遥测：syn[帧同步码]，值为：449838109(1acffc1dH)\n" +
                "\t成立条件：\n" +
                "\t syn==449838109\n" +
                "\t</DI>\n" +
                "                            <R>Rule_137605_SYN_sub_1</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>0</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table28\" msdata:rowOrder=\"27\">\n" +
                "                            <I>103</I>\n" +
                "                            <FT>2021-01-18T17:52:14+08:00</FT>\n" +
                "                            <DT>2021-01-18T17:52:47+08:00</DT>\n" +
                "                            <DI>信息：KBZ12_天线遮挡太敏标志(RKSB472)跳变为遮挡!\n" +
                "等级：正常\n" +
                "\t信源：0x3a150f00\n" +
                "\t诊断参数：\n" +
                "\t遥测：rksb472[KBZ12_天线遮挡太敏标志]，值为：1(1H)\n" +
                "\t成立条件：\n" +
                "\t rksb472==1\n" +
                "\t</DI>\n" +
                "                            <R>Rule_133068_RKSB472</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>0</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table29\" msdata:rowOrder=\"28\">\n" +
                "                            <I>106</I>\n" +
                "                            <FT>2021-01-19T00:35:08+08:00</FT>\n" +
                "                            <DT>2021-01-19T03:59:43+08:00</DT>\n" +
                "                            <DI>-</DI>\n" +
                "                            <R>Rule_133068_RKSB472</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>1</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table30\" msdata:rowOrder=\"29\">\n" +
                "                            <I>110</I>\n" +
                "                            <FT>2021-01-19T09:43:31+08:00</FT>\n" +
                "                            <DT>2021-01-19T09:49:12+08:00</DT>\n" +
                "                            <DI>-</DI>\n" +
                "                            <R>Rule_137762_RDSP155</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>1</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table31\" msdata:rowOrder=\"30\">\n" +
                "                            <I>115</I>\n" +
                "                            <FT>2021-01-18T02:09:01+08:00</FT>\n" +
                "                            <DT>2021-01-18T02:09:04+08:00</DT>\n" +
                "                            <DI>信息：帧同步码(SYN)正确!\n" +
                "等级：正常\n" +
                "\t信源：0x3a150f00\n" +
                "\t诊断参数：\n" +
                "\t遥测：syn[帧同步码]，值为：449838109(1acffc1dH)\n" +
                "\t成立条件：\n" +
                "\t syn==449838109\n" +
                "\t</DI>\n" +
                "                            <R>Rule_137605_SYN_sub_1</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>0</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table32\" msdata:rowOrder=\"31\">\n" +
                "                            <I>117</I>\n" +
                "                            <FT>2021-01-18T02:23:20+08:00</FT>\n" +
                "                            <DT>2021-01-18T02:23:56+08:00</DT>\n" +
                "                            <DI>信息：KBZ12_天线遮挡太敏标志(RKSB472)跳变为遮挡!\n" +
                "等级：正常\n" +
                "\t信源：0x3a150f00\n" +
                "\t诊断参数：\n" +
                "\t遥测：rksb472[KBZ12_天线遮挡太敏标志]，值为：1(1H)\n" +
                "\t成立条件：\n" +
                "\t rksb472==1\n" +
                "\t</DI>\n" +
                "                            <R>Rule_133068_RKSB472</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>0</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table33\" msdata:rowOrder=\"32\">\n" +
                "                            <I>118</I>\n" +
                "                            <FT>2021-01-18T02:23:20+08:00</FT>\n" +
                "                            <DT>2021-01-18T05:48:49+08:00</DT>\n" +
                "                            <DI>-</DI>\n" +
                "                            <R>Rule_133068_RKSB472</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>1</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table34\" msdata:rowOrder=\"33\">\n" +
                "                            <I>119</I>\n" +
                "                            <FT>2021-01-18T19:41:37+08:00</FT>\n" +
                "                            <DT>2021-01-18T19:42:10+08:00</DT>\n" +
                "                            <DI>信息：KBZ12_天线遮挡太敏标志(RKSB472)跳变为遮挡!\n" +
                "等级：正常\n" +
                "\t信源：0x3a150f00\n" +
                "\t诊断参数：\n" +
                "\t遥测：rksb472[KBZ12_天线遮挡太敏标志]，值为：1(1H)\n" +
                "\t成立条件：\n" +
                "\t rksb472==1\n" +
                "\t</DI>\n" +
                "                            <R>Rule_133068_RKSB472</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>0</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table35\" msdata:rowOrder=\"34\">\n" +
                "                            <I>120</I>\n" +
                "                            <FT>2021-01-18T19:41:37+08:00</FT>\n" +
                "                            <DT>2021-01-18T23:33:23+08:00</DT>\n" +
                "                            <DI>-</DI>\n" +
                "                            <R>Rule_133068_RKSB472</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>1</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table36\" msdata:rowOrder=\"35\">\n" +
                "                            <I>121</I>\n" +
                "                            <FT>2021-01-19T02:24:30+08:00</FT>\n" +
                "                            <DT>2021-01-19T02:25:06+08:00</DT>\n" +
                "                            <DI>信息：KBZ12_天线遮挡太敏标志(RKSB472)跳变为遮挡!\n" +
                "等级：正常\n" +
                "\t信源：0x66061608\n" +
                "\t诊断参数：\n" +
                "\t遥测：rksb472[KBZ12_天线遮挡太敏标志]，值为：1(1H)\n" +
                "\t成立条件：\n" +
                "\t rksb472==1\n" +
                "\t</DI>\n" +
                "                            <R>Rule_133068_RKSB472</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>0</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table37\" msdata:rowOrder=\"36\">\n" +
                "                            <I>122</I>\n" +
                "                            <FT>2021-01-19T02:24:30+08:00</FT>\n" +
                "                            <DT>2021-01-19T05:49:06+08:00</DT>\n" +
                "                            <DI>-</DI>\n" +
                "                            <R>Rule_133068_RKSB472</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>1</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table38\" msdata:rowOrder=\"37\">\n" +
                "                            <I>127</I>\n" +
                "                            <FT>2021-01-19T19:41:52+08:00</FT>\n" +
                "                            <DT>2021-01-19T19:42:24+08:00</DT>\n" +
                "                            <DI>信息：KBZ12_天线遮挡太敏标志(RKSB472)跳变为遮挡!\n" +
                "等级：正常\n" +
                "\t信源：0x3a150f00\n" +
                "\t诊断参数：\n" +
                "\t遥测：rksb472[KBZ12_天线遮挡太敏标志]，值为：1(1H)\n" +
                "\t成立条件：\n" +
                "\t rksb472==1\n" +
                "\t</DI>\n" +
                "                            <R>Rule_133068_RKSB472</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>0</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table39\" msdata:rowOrder=\"38\">\n" +
                "                            <I>129</I>\n" +
                "                            <FT>2021-01-22T01:39:07+08:00</FT>\n" +
                "                            <DT>2021-01-22T01:39:08+08:00</DT>\n" +
                "                            <DI>信息：帧同步码(SYN)正确!\n" +
                "等级：正常\n" +
                "\t信源：0x66030e08\n" +
                "\t诊断参数：\n" +
                "\t遥测：syn[帧同步码]，值为：449838109(1acffc1dH)\n" +
                "\t成立条件：\n" +
                "\t syn==449838109\n" +
                "\t</DI>\n" +
                "                            <R>Rule_137605_SYN_sub_1</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>0</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table40\" msdata:rowOrder=\"39\">\n" +
                "                            <I>130</I>\n" +
                "                            <FT>2021-01-22T01:39:07+08:00</FT>\n" +
                "                            <DT>2021-01-22T01:39:08+08:00</DT>\n" +
                "                            <DI>信息：航天器标识(SCID)为251!\n" +
                "等级：正常\n" +
                "\t信源：0x66030e08\n" +
                "\t诊断参数：\n" +
                "\t遥测：scid[航天器标识]，值为：251(fbH)\n" +
                "\t成立条件：\n" +
                "\tscid(=251) == 251 \n" +
                "\n" +
                "\t</DI>\n" +
                "                            <R>Rule_137604_SCID_sub_1</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>0</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table41\" msdata:rowOrder=\"40\">\n" +
                "                            <I>5</I>\n" +
                "                            <FT>2021-02-03T15:13:07+08:00</FT>\n" +
                "                            <DT>2021-02-03T15:13:43+08:00</DT>\n" +
                "                            <DI>信息：KBZ12_天线遮挡太敏标志(RKSB472)跳变为遮挡!\n" +
                "等级：正常\n" +
                "\t信源：0x3a150f00\n" +
                "\t诊断参数：\n" +
                "\t遥测：rksb472[KBZ12_天线遮挡太敏标志]，值为：1(1H)\n" +
                "\t成立条件：\n" +
                "\t rksb472==1\n" +
                "\t</DI>\n" +
                "                            <R>Rule_133068_RKSB472</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>0</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table42\" msdata:rowOrder=\"41\">\n" +
                "                            <I>6</I>\n" +
                "                            <FT>2021-02-03T15:13:07+08:00</FT>\n" +
                "                            <DT>2021-02-03T15:20:13+08:00</DT>\n" +
                "                            <DI>-</DI>\n" +
                "                            <R>Rule_133068_RKSB472</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>1</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table43\" msdata:rowOrder=\"42\">\n" +
                "                            <I>8</I>\n" +
                "                            <FT>2021-02-03T15:33:58+08:00</FT>\n" +
                "                            <DT>2021-02-03T18:59:26+08:00</DT>\n" +
                "                            <DI>-</DI>\n" +
                "                            <R>Rule_133068_RKSB472</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>1</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table44\" msdata:rowOrder=\"43\">\n" +
                "                            <I>17</I>\n" +
                "                            <FT>2021-01-18T00:14:19+08:00</FT>\n" +
                "                            <DT>2021-01-18T00:14:54+08:00</DT>\n" +
                "                            <DI>信息：KBZ12_天线遮挡太敏标志(RKSB472)跳变为遮挡!\n" +
                "等级：正常\n" +
                "\t信源：0x3a150f00\n" +
                "\t诊断参数：\n" +
                "\t遥测：rksb472[KBZ12_天线遮挡太敏标志]，值为：1(1H)\n" +
                "\t成立条件：\n" +
                "\t rksb472==1\n" +
                "\t</DI>\n" +
                "                            <R>Rule_133068_RKSB472</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>0</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table45\" msdata:rowOrder=\"44\">\n" +
                "                            <I>21</I>\n" +
                "                            <FT>2021-01-19T00:15:29+08:00</FT>\n" +
                "                            <DT>2021-01-19T00:16:04+08:00</DT>\n" +
                "                            <DI>信息：KBZ12_天线遮挡太敏标志(RKSB472)跳变为遮挡!\n" +
                "等级：正常\n" +
                "\t信源：0x66061608\n" +
                "\t诊断参数：\n" +
                "\t遥测：rksb472[KBZ12_天线遮挡太敏标志]，值为：1(1H)\n" +
                "\t成立条件：\n" +
                "\t rksb472==1\n" +
                "\t</DI>\n" +
                "                            <R>Rule_133068_RKSB472</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>0</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table46\" msdata:rowOrder=\"45\">\n" +
                "                            <I>25</I>\n" +
                "                            <FT>2021-01-19T09:25:06+08:00</FT>\n" +
                "                            <DT>2021-01-19T09:29:22+08:00</DT>\n" +
                "                            <DI>-</DI>\n" +
                "                            <R>Rule_137765_RDSP158</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>1</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table47\" msdata:rowOrder=\"46\">\n" +
                "                            <I>26</I>\n" +
                "                            <FT>2021-01-19T09:23:52+08:00</FT>\n" +
                "                            <DT>2021-01-19T09:29:33+08:00</DT>\n" +
                "                            <DI>-</DI>\n" +
                "                            <R>Rule_137762_RDSP155</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>1</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table48\" msdata:rowOrder=\"47\">\n" +
                "                            <I>28</I>\n" +
                "                            <FT>2021-01-19T17:32:50+08:00</FT>\n" +
                "                            <DT>2021-01-19T21:23:58+08:00</DT>\n" +
                "                            <DI>-</DI>\n" +
                "                            <R>Rule_133068_RKSB472</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>1</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table49\" msdata:rowOrder=\"48\">\n" +
                "                            <I>29</I>\n" +
                "                            <FT>2021-01-21T23:30:05+08:00</FT>\n" +
                "                            <DT>2021-01-21T23:30:06+08:00</DT>\n" +
                "                            <DI>信息：航天器标识(SCID)为251!\n" +
                "等级：正常\n" +
                "\t信源：0x66030e08\n" +
                "\t诊断参数：\n" +
                "\t遥测：scid[航天器标识]，值为：251(fbH)\n" +
                "\t成立条件：\n" +
                "\tscid(=251) == 251 \n" +
                "\n" +
                "\t</DI>\n" +
                "                            <R>Rule_137604_SCID_sub_1</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>0</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table50\" msdata:rowOrder=\"49\">\n" +
                "                            <I>54</I>\n" +
                "                            <FT>2021-01-18T00:04:25+08:00</FT>\n" +
                "                            <DT>2021-01-18T00:04:29+08:00</DT>\n" +
                "                            <DI>信息：帧同步码(SYN)正确!\n" +
                "等级：正常\n" +
                "\t信源：0x3a150f00\n" +
                "\t诊断参数：\n" +
                "\t遥测：syn[帧同步码]，值为：449838109(1acffc1dH)\n" +
                "\t成立条件：\n" +
                "\t syn==449838109\n" +
                "\t</DI>\n" +
                "                            <R>Rule_137605_SYN_sub_1</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>0</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table51\" msdata:rowOrder=\"50\">\n" +
                "                            <I>55</I>\n" +
                "                            <FT>2021-01-18T00:18:45+08:00</FT>\n" +
                "                            <DT>2021-01-18T00:19:20+08:00</DT>\n" +
                "                            <DI>信息：KBZ12_天线遮挡太敏标志(RKSB472)跳变为遮挡!\n" +
                "等级：正常\n" +
                "\t信源：0x3a150f00\n" +
                "\t诊断参数：\n" +
                "\t遥测：rksb472[KBZ12_天线遮挡太敏标志]，值为：1(1H)\n" +
                "\t成立条件：\n" +
                "\t rksb472==1\n" +
                "\t</DI>\n" +
                "                            <R>Rule_133068_RKSB472</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>0</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table52\" msdata:rowOrder=\"51\">\n" +
                "                            <I>57</I>\n" +
                "                            <FT>2021-01-18T17:37:01+08:00</FT>\n" +
                "                            <DT>2021-01-18T17:37:34+08:00</DT>\n" +
                "                            <DI>信息：KBZ12_天线遮挡太敏标志(RKSB472)跳变为遮挡!\n" +
                "等级：正常\n" +
                "\t信源：0x3a150f00\n" +
                "\t诊断参数：\n" +
                "\t遥测：rksb472[KBZ12_天线遮挡太敏标志]，值为：1(1H)\n" +
                "\t成立条件：\n" +
                "\t rksb472==1\n" +
                "\t</DI>\n" +
                "                            <R>Rule_133068_RKSB472</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>0</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table53\" msdata:rowOrder=\"52\">\n" +
                "                            <I>59</I>\n" +
                "                            <FT>2021-01-19T00:19:55+08:00</FT>\n" +
                "                            <DT>2021-01-19T00:20:30+08:00</DT>\n" +
                "                            <DI>信息：KBZ12_天线遮挡太敏标志(RKSB472)跳变为遮挡!\n" +
                "等级：正常\n" +
                "\t信源：0x66061608\n" +
                "\t诊断参数：\n" +
                "\t遥测：rksb472[KBZ12_天线遮挡太敏标志]，值为：1(1H)\n" +
                "\t成立条件：\n" +
                "\t rksb472==1\n" +
                "\t</DI>\n" +
                "                            <R>Rule_133068_RKSB472</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>0</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table54\" msdata:rowOrder=\"53\">\n" +
                "                            <I>65</I>\n" +
                "                            <FT>2021-01-19T17:37:16+08:00</FT>\n" +
                "                            <DT>2021-01-19T17:37:49+08:00</DT>\n" +
                "                            <DI>信息：KBZ12_天线遮挡太敏标志(RKSB472)跳变为遮挡!\n" +
                "等级：正常\n" +
                "\t信源：0x3a150f00\n" +
                "\t诊断参数：\n" +
                "\t遥测：rksb472[KBZ12_天线遮挡太敏标志]，值为：1(1H)\n" +
                "\t成立条件：\n" +
                "\t rksb472==1\n" +
                "\t</DI>\n" +
                "                            <R>Rule_133068_RKSB472</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>0</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table55\" msdata:rowOrder=\"54\">\n" +
                "                            <I>79</I>\n" +
                "                            <FT>2021-01-22T17:14:40+08:00</FT>\n" +
                "                            <DT>2021-01-22T17:15:12+08:00</DT>\n" +
                "                            <DI>信息：KBZ12_天线遮挡太敏标志(RKSB472)跳变为遮挡!\n" +
                "等级：正常\n" +
                "\t信源：0x3a150f00\n" +
                "\t诊断参数：\n" +
                "\t遥测：rksb472[KBZ12_天线遮挡太敏标志]，值为：1(1H)\n" +
                "\t成立条件：\n" +
                "\t rksb472==1\n" +
                "\t</DI>\n" +
                "                            <R>Rule_133068_RKSB472</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>0</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table56\" msdata:rowOrder=\"55\">\n" +
                "                            <I>83</I>\n" +
                "                            <FT>2021-01-18T00:00:10+08:00</FT>\n" +
                "                            <DT>2021-01-18T00:00:14+08:00</DT>\n" +
                "                            <DI>信息：航天器标识(SCID)为251!\n" +
                "等级：正常\n" +
                "\t信源：0x3a150f00\n" +
                "\t诊断参数：\n" +
                "\t遥测：scid[航天器标识]，值为：251(fbH)\n" +
                "\t成立条件：\n" +
                "\tscid(=251) == 251 \n" +
                "\n" +
                "\t</DI>\n" +
                "                            <R>Rule_137604_SCID_sub_1</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>0</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table57\" msdata:rowOrder=\"56\">\n" +
                "                            <I>96</I>\n" +
                "                            <FT>2021-01-19T17:33:01+08:00</FT>\n" +
                "                            <DT>2021-01-19T21:24:09+08:00</DT>\n" +
                "                            <DI>-</DI>\n" +
                "                            <R>Rule_133068_RKSB472</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>1</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table58\" msdata:rowOrder=\"57\">\n" +
                "                            <I>100</I>\n" +
                "                            <FT>2021-01-25T06:19:07+08:00</FT>\n" +
                "                            <DT>2021-01-25T06:19:10+08:00</DT>\n" +
                "                            <DI>信息：帧同步码(SYN)正确!\n" +
                "等级：正常\n" +
                "\t信源：0x3a150f00\n" +
                "\t诊断参数：\n" +
                "\t遥测：syn[帧同步码]，值为：449838109(1acffc1dH)\n" +
                "\t成立条件：\n" +
                "\t syn==449838109\n" +
                "\t</DI>\n" +
                "                            <R>Rule_137605_SYN_sub_1</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>0</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table59\" msdata:rowOrder=\"58\">\n" +
                "                            <I>102</I>\n" +
                "                            <FT>2021-01-18T00:33:58+08:00</FT>\n" +
                "                            <DT>2021-01-18T03:59:26+08:00</DT>\n" +
                "                            <DI>-</DI>\n" +
                "                            <R>Rule_133068_RKSB472</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>1</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table60\" msdata:rowOrder=\"59\">\n" +
                "                            <I>105</I>\n" +
                "                            <FT>2021-01-19T00:35:08+08:00</FT>\n" +
                "                            <DT>2021-01-19T00:35:43+08:00</DT>\n" +
                "                            <DI>信息：KBZ12_天线遮挡太敏标志(RKSB472)跳变为遮挡!\n" +
                "等级：正常\n" +
                "\t信源：0x66061608\n" +
                "\t诊断参数：\n" +
                "\t遥测：rksb472[KBZ12_天线遮挡太敏标志]，值为：1(1H)\n" +
                "\t成立条件：\n" +
                "\t rksb472==1\n" +
                "\t</DI>\n" +
                "                            <R>Rule_133068_RKSB472</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>0</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table61\" msdata:rowOrder=\"60\">\n" +
                "                            <I>107</I>\n" +
                "                            <FT>2021-01-19T09:43:31+08:00</FT>\n" +
                "                            <DT>2021-01-19T09:43:50+08:00</DT>\n" +
                "                            <DI>信息：SP120VC8选址组合数据包3使能标识(RDSP155)由禁止跳变为使能!\n" +
                "等级：一般\n" +
                "\t信源：0x3a150f00\n" +
                "\t诊断参数：\n" +
                "\t遥测：rdsp155[SP120VC8选址组合数据包3使能标识]，值为：1(1H)\n" +
                "\t成立条件：\n" +
                "\t rdsp155==1\n" +
                "\t</DI>\n" +
                "                            <R>Rule_137762_RDSP155</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>0</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table62\" msdata:rowOrder=\"61\">\n" +
                "                            <I>108</I>\n" +
                "                            <FT>2021-01-19T09:44:45+08:00</FT>\n" +
                "                            <DT>2021-01-19T09:45:03+08:00</DT>\n" +
                "                            <DI>信息：SP129DPRU 内存下载数据包使能标识(RDSP158)由禁止跳变为使能!\n" +
                "等级：一般\n" +
                "\t信源：0x3a150f00\n" +
                "\t诊断参数：\n" +
                "\t遥测：rdsp158[SP129DPRU 内存下载数据包使能标识]，值为：1(1H)\n" +
                "\t成立条件：\n" +
                "\t rdsp158==1\n" +
                "\t</DI>\n" +
                "                            <R>Rule_137765_RDSP158</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>0</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table63\" msdata:rowOrder=\"62\">\n" +
                "                            <I>109</I>\n" +
                "                            <FT>2021-01-19T09:44:45+08:00</FT>\n" +
                "                            <DT>2021-01-19T09:49:02+08:00</DT>\n" +
                "                            <DI>-</DI>\n" +
                "                            <R>Rule_137765_RDSP158</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>1</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table64\" msdata:rowOrder=\"63\">\n" +
                "                            <I>112</I>\n" +
                "                            <FT>2021-01-19T17:52:30+08:00</FT>\n" +
                "                            <DT>2021-01-19T21:43:37+08:00</DT>\n" +
                "                            <DI>-</DI>\n" +
                "                            <R>Rule_133068_RKSB472</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>1</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table65\" msdata:rowOrder=\"64\">\n" +
                "                            <I>128</I>\n" +
                "                            <FT>2021-01-19T19:41:52+08:00</FT>\n" +
                "                            <DT>2021-01-19T23:33:00+08:00</DT>\n" +
                "                            <DI>-</DI>\n" +
                "                            <R>Rule_133068_RKSB472</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>1</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table66\" msdata:rowOrder=\"65\">\n" +
                "                            <I>13</I>\n" +
                "                            <FT>2021-01-18T16:36:58+08:00</FT>\n" +
                "                            <DT>2021-01-18T16:37:34+08:00</DT>\n" +
                "                            <DI>信息：KBZ12_天线遮挡太敏标志(RKSB472)跳变为遮挡!\n" +
                "等级：正常\n" +
                "\t信源：0x3a150f00\n" +
                "\t诊断参数：\n" +
                "\t遥测：rksb472[KBZ12_天线遮挡太敏标志]，值为：1(1H)\n" +
                "\t成立条件：\n" +
                "\t rksb472==1\n" +
                "\t</DI>\n" +
                "                            <R>Rule_133068_RKSB472</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>0</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table67\" msdata:rowOrder=\"66\">\n" +
                "                            <I>15</I>\n" +
                "                            <FT>2021-01-18T00:14:57+08:00</FT>\n" +
                "                            <DT>2021-01-18T00:15:32+08:00</DT>\n" +
                "                            <DI>信息：KBZ12_天线遮挡太敏标志(RKSB472)跳变为遮挡!\n" +
                "等级：正常\n" +
                "\t信源：0x3a150f00\n" +
                "\t诊断参数：\n" +
                "\t遥测：rksb472[KBZ12_天线遮挡太敏标志]，值为：1(1H)\n" +
                "\t成立条件：\n" +
                "\t rksb472==1\n" +
                "\t</DI>\n" +
                "                            <R>Rule_133068_RKSB472</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>0</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table68\" msdata:rowOrder=\"67\">\n" +
                "                            <I>1</I>\n" +
                "                            <FT>2021-02-02T16:20:15+08:00</FT>\n" +
                "                            <DT>2021-02-02T16:20:19+08:00</DT>\n" +
                "                            <DI>信息：帧同步码(SYN)正确!\n" +
                "等级：正常\n" +
                "\t信源：0x3a150f00\n" +
                "\t诊断参数：\n" +
                "\t遥测：syn[帧同步码]，值为：449838109(1acffc1dH)\n" +
                "\t成立条件：\n" +
                "\t syn==449838109\n" +
                "\t</DI>\n" +
                "                            <R>Rule_137605_SYN_sub_1</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>0</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table69\" msdata:rowOrder=\"68\">\n" +
                "                            <I>7</I>\n" +
                "                            <FT>2021-02-03T15:33:58+08:00</FT>\n" +
                "                            <DT>2021-02-03T15:34:33+08:00</DT>\n" +
                "                            <DI>信息：KBZ12_天线遮挡太敏标志(RKSB472)跳变为遮挡!\n" +
                "等级：正常\n" +
                "\t信源：0x3a150f00\n" +
                "\t诊断参数：\n" +
                "\t遥测：rksb472[KBZ12_天线遮挡太敏标志]，值为：1(1H)\n" +
                "\t成立条件：\n" +
                "\t rksb472==1\n" +
                "\t</DI>\n" +
                "                            <R>Rule_133068_RKSB472</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>0</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table70\" msdata:rowOrder=\"69\">\n" +
                "                            <I>9</I>\n" +
                "                            <FT>2021-02-04T10:22:38+08:00</FT>\n" +
                "                            <DT>2021-02-04T10:22:41+08:00</DT>\n" +
                "                            <DI>信息：帧同步码(SYN)正确!\n" +
                "等级：正常\n" +
                "\t信源：0x3a150f00\n" +
                "\t诊断参数：\n" +
                "\t遥测：syn[帧同步码]，值为：449838109(1acffc1dH)\n" +
                "\t成立条件：\n" +
                "\t syn==449838109\n" +
                "\t</DI>\n" +
                "                            <R>Rule_137605_SYN_sub_1</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>0</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table71\" msdata:rowOrder=\"70\">\n" +
                "                            <I>11</I>\n" +
                "                            <FT>2021-02-04T10:36:57+08:00</FT>\n" +
                "                            <DT>2021-02-04T10:37:33+08:00</DT>\n" +
                "                            <DI>信息：KBZ12_天线遮挡太敏标志(RKSB472)跳变为遮挡!\n" +
                "等级：正常\n" +
                "\t信源：0x3a150f00\n" +
                "\t诊断参数：\n" +
                "\t遥测：rksb472[KBZ12_天线遮挡太敏标志]，值为：1(1H)\n" +
                "\t成立条件：\n" +
                "\t rksb472==1\n" +
                "\t</DI>\n" +
                "                            <R>Rule_133068_RKSB472</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>0</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table72\" msdata:rowOrder=\"71\">\n" +
                "                            <I>12</I>\n" +
                "                            <FT>2021-02-04T10:36:57+08:00</FT>\n" +
                "                            <DT>2021-02-04T14:02:25+08:00</DT>\n" +
                "                            <DI>-</DI>\n" +
                "                            <R>Rule_133068_RKSB472</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>1</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table73\" msdata:rowOrder=\"72\">\n" +
                "                            <I>14</I>\n" +
                "                            <FT>2021-01-18T16:36:58+08:00</FT>\n" +
                "                            <DT>2021-01-18T00:01:12+08:00</DT>\n" +
                "                            <DI>-</DI>\n" +
                "                            <R>Rule_133068_RKSB472</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>1</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table74\" msdata:rowOrder=\"73\">\n" +
                "                            <I>20</I>\n" +
                "                            <FT>2021-01-18T17:32:35+08:00</FT>\n" +
                "                            <DT>2021-01-18T21:24:21+08:00</DT>\n" +
                "                            <DI>-</DI>\n" +
                "                            <R>Rule_133068_RKSB472</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>1</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table75\" msdata:rowOrder=\"74\">\n" +
                "                            <I>22</I>\n" +
                "                            <FT>2021-01-19T00:15:29+08:00</FT>\n" +
                "                            <DT>2021-01-19T03:40:04+08:00</DT>\n" +
                "                            <DI>-</DI>\n" +
                "                            <R>Rule_133068_RKSB472</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>1</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table76\" msdata:rowOrder=\"75\">\n" +
                "                            <I>30</I>\n" +
                "                            <FT>2021-01-21T23:30:05+08:00</FT>\n" +
                "                            <DT>2021-01-21T23:30:06+08:00</DT>\n" +
                "                            <DI>信息：帧同步码(SYN)正确!\n" +
                "等级：正常\n" +
                "\t信源：0x66030e08\n" +
                "\t诊断参数：\n" +
                "\t遥测：syn[帧同步码]，值为：449838109(1acffc1dH)\n" +
                "\t成立条件：\n" +
                "\t syn==449838109\n" +
                "\t</DI>\n" +
                "                            <R>Rule_137605_SYN_sub_1</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>0</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table77\" msdata:rowOrder=\"76\">\n" +
                "                            <I>38</I>\n" +
                "                            <FT>2021-01-18T00:00:18+08:00</FT>\n" +
                "                            <DT>2021-01-18T00:00:22+08:00</DT>\n" +
                "                            <DI>信息：航天器标识(SCID)为251!\n" +
                "等级：正常\n" +
                "\t信源：0x3a150f00\n" +
                "\t诊断参数：\n" +
                "\t遥测：scid[航天器标识]，值为：251(fbH)\n" +
                "\t成立条件：\n" +
                "\tscid(=251) == 251 \n" +
                "\n" +
                "\t</DI>\n" +
                "                            <R>Rule_137604_SCID_sub_1</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>0</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table78\" msdata:rowOrder=\"77\">\n" +
                "                            <I>51</I>\n" +
                "                            <FT>2021-01-21T23:30:25+08:00</FT>\n" +
                "                            <DT>2021-01-21T23:30:25+08:00</DT>\n" +
                "                            <DI>信息：航天器标识(SCID)为251!\n" +
                "等级：正常\n" +
                "\t信源：0x66030e08\n" +
                "\t诊断参数：\n" +
                "\t遥测：scid[航天器标识]，值为：251(fbH)\n" +
                "\t成立条件：\n" +
                "\tscid(=251) == 251 \n" +
                "\n" +
                "\t</DI>\n" +
                "                            <R>Rule_137604_SCID_sub_1</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>0</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table79\" msdata:rowOrder=\"78\">\n" +
                "                            <I>58</I>\n" +
                "                            <FT>2021-01-18T17:37:01+08:00</FT>\n" +
                "                            <DT>2021-01-18T21:28:47+08:00</DT>\n" +
                "                            <DI>-</DI>\n" +
                "                            <R>Rule_133068_RKSB472</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>1</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table80\" msdata:rowOrder=\"79\">\n" +
                "                            <I>62</I>\n" +
                "                            <FT>2021-01-19T09:29:32+08:00</FT>\n" +
                "                            <DT>2021-01-19T09:29:49+08:00</DT>\n" +
                "                            <DI>信息：SP129DPRU 内存下载数据包使能标识(RDSP158)由禁止跳变为使能!\n" +
                "等级：一般\n" +
                "\t信源：0x3a150f00\n" +
                "\t诊断参数：\n" +
                "\t遥测：rdsp158[SP129DPRU 内存下载数据包使能标识]，值为：1(1H)\n" +
                "\t成立条件：\n" +
                "\t rdsp158==1\n" +
                "\t</DI>\n" +
                "                            <R>Rule_137765_RDSP158</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>0</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table81\" msdata:rowOrder=\"80\">\n" +
                "                            <I>63</I>\n" +
                "                            <FT>2021-01-19T09:29:32+08:00</FT>\n" +
                "                            <DT>2021-01-19T09:33:48+08:00</DT>\n" +
                "                            <DI>-</DI>\n" +
                "                            <R>Rule_137765_RDSP158</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>1</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table82\" msdata:rowOrder=\"81\">\n" +
                "                            <I>66</I>\n" +
                "                            <FT>2021-01-19T17:37:16+08:00</FT>\n" +
                "                            <DT>2021-01-19T21:28:24+08:00</DT>\n" +
                "                            <DI>-</DI>\n" +
                "                            <R>Rule_133068_RKSB472</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>1</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table83\" msdata:rowOrder=\"82\">\n" +
                "                            <I>68</I>\n" +
                "                            <FT>2021-01-20T23:41:49+08:00</FT>\n" +
                "                            <DT>2021-01-20T23:41:52+08:00</DT>\n" +
                "                            <DI>信息：航天器标识(SCID)为251!\n" +
                "等级：正常\n" +
                "\t信源：0x3a150f00\n" +
                "\t诊断参数：\n" +
                "\t遥测：scid[航天器标识]，值为：251(fbH)\n" +
                "\t成立条件：\n" +
                "\tscid(=251) == 251 \n" +
                "\n" +
                "\t</DI>\n" +
                "                            <R>Rule_137604_SCID_sub_1</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>0</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table84\" msdata:rowOrder=\"83\">\n" +
                "                            <I>69</I>\n" +
                "                            <FT>2021-01-20T23:56:08+08:00</FT>\n" +
                "                            <DT>2021-01-20T23:56:44+08:00</DT>\n" +
                "                            <DI>信息：KBZ12_天线遮挡太敏标志(RKSB472)跳变为遮挡!\n" +
                "等级：正常\n" +
                "\t信源：0x3a150f00\n" +
                "\t诊断参数：\n" +
                "\t遥测：rksb472[KBZ12_天线遮挡太敏标志]，值为：1(1H)\n" +
                "\t成立条件：\n" +
                "\t rksb472==1\n" +
                "\t</DI>\n" +
                "                            <R>Rule_133068_RKSB472</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>0</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table85\" msdata:rowOrder=\"84\">\n" +
                "                            <I>87</I>\n" +
                "                            <FT>2021-01-18T17:32:46+08:00</FT>\n" +
                "                            <DT>2021-01-18T17:33:19+08:00</DT>\n" +
                "                            <DI>信息：KBZ12_天线遮挡太敏标志(RKSB472)跳变为遮挡!\n" +
                "等级：正常\n" +
                "\t信源：0x3a150f00\n" +
                "\t诊断参数：\n" +
                "\t遥测：rksb472[KBZ12_天线遮挡太敏标志]，值为：1(1H)\n" +
                "\t成立条件：\n" +
                "\t rksb472==1\n" +
                "\t</DI>\n" +
                "                            <R>Rule_133068_RKSB472</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>0</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table86\" msdata:rowOrder=\"85\">\n" +
                "                            <I>99</I>\n" +
                "                            <FT>2021-01-25T06:19:07+08:00</FT>\n" +
                "                            <DT>2021-01-25T06:19:10+08:00</DT>\n" +
                "                            <DI>信息：航天器标识(SCID)为251!\n" +
                "等级：正常\n" +
                "\t信源：0x3a150f00\n" +
                "\t诊断参数：\n" +
                "\t遥测：scid[航天器标识]，值为：251(fbH)\n" +
                "\t成立条件：\n" +
                "\tscid(=251) == 251 \n" +
                "\n" +
                "\t</DI>\n" +
                "                            <R>Rule_137604_SCID_sub_1</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>0</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table87\" msdata:rowOrder=\"86\">\n" +
                "                            <I>101</I>\n" +
                "                            <FT>2021-01-18T00:33:58+08:00</FT>\n" +
                "                            <DT>2021-01-18T00:34:33+08:00</DT>\n" +
                "                            <DI>信息：KBZ12_天线遮挡太敏标志(RKSB472)跳变为遮挡!\n" +
                "等级：正常\n" +
                "\t信源：0x3a150f00\n" +
                "\t诊断参数：\n" +
                "\t遥测：rksb472[KBZ12_天线遮挡太敏标志]，值为：1(1H)\n" +
                "\t成立条件：\n" +
                "\t rksb472==1\n" +
                "\t</DI>\n" +
                "                            <R>Rule_133068_RKSB472</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>0</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table88\" msdata:rowOrder=\"87\">\n" +
                "                            <I>104</I>\n" +
                "                            <FT>2021-01-18T17:52:14+08:00</FT>\n" +
                "                            <DT>2021-01-18T21:44:00+08:00</DT>\n" +
                "                            <DI>-</DI>\n" +
                "                            <R>Rule_133068_RKSB472</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>1</ST>\n" +
                "                        </Table>\n" +
                "                        <Table diffgr:id=\"Table89\" msdata:rowOrder=\"88\">\n" +
                "                            <I>123</I>\n" +
                "                            <FT>2021-01-19T11:32:54+08:00</FT>\n" +
                "                            <DT>2021-01-19T11:33:12+08:00</DT>\n" +
                "                            <DI>信息：SP120VC8选址组合数据包3使能标识(RDSP155)由禁止跳变为使能!\n" +
                "等级：一般\n" +
                "\t信源：0x3a150f00\n" +
                "\t诊断参数：\n" +
                "\t遥测：rdsp155[SP120VC8选址组合数据包3使能标识]，值为：1(1H)\n" +
                "\t成立条件：\n" +
                "\t rdsp155==1\n" +
                "\t</DI>\n" +
                "                            <R>Rule_137762_RDSP155</R>\n" +
                "                            <S>实时</S>\n" +
                "                            <SN>1</SN>\n" +
                "                            <ST>0</ST>\n" +
                "                        </Table>\n" +
                "                    </NewDataSet>\n" +
                "                </diffgr:diffgram>\n" +
                "            </QueryFAUDS_RESULTResult>\n" +
                "        </QueryFAUDS_RESULTResponse>\n" +
                "    </soap:Body>\n" +
                "</soap:Envelope>";

        xml=xml.replaceAll(":","");
//        xml=xml.replaceAll("soapEnvelope","soap:Envelope");
        System.out.println(xml);


        JSONObject object = XML.toJSONObject(xml);

        String str_object = object.toString();

//        Map<String, Object> jsonMap=JsonUtils.transferToMap(str_object);

//        System.out.println(jsonMap);
//
//        String rule="soap:Envelope.soap:Body.QueryFAUDS_RESULTResponse.QueryFAUDS_RESULTResult.diffgr:diffgram.NewDataSet.Table.{S}";
//        rule=rule.replaceAll(":","");
//        List e = JsonUtils.getValue(str_object, rule, List.class);
//        System.out.println(e);

    }
}
