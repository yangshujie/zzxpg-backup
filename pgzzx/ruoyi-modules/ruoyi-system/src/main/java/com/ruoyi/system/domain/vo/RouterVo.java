package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

/**
 * 路由配置信息
 * 
 * @author ruoyi
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Data
//@JsonInclude这个注解过滤掉了当返回值为null 的时候的属性。
//@JsonInclude(JsonInclude.Include.ALWAYS) 默认
//@JsonInclude(JsonInclude.Include.NON_DEFAULT ) 属性为默认值不序列化
//@JsonInclude(JsonInclude.Include.NON_EMPTY ) 属性为空（""） 或者为 NULL 都不序列化
//@JsonInclude(JsonInclude.Include.NON_NULL ) 属性为NULL 不序列化
public class RouterVo
{
    /**
     * 路由名字
     */
    private String name;

    /**
     * 路由地址
     */
    private String path;

    /**
     * 是否隐藏路由，当设置 true 的时候该路由不会再侧边栏出现
     */
    private boolean hidden;

    /**
     * 重定向地址，当设置 noRedirect 的时候该路由在面包屑导航中不可被点击
     */
    private String redirect;

    /**
     * 组件地址
     */
    private String component;

    /**
     * 路由参数：如 {"id": 1, "name": "ry"}
     */
    private String query;

    /**
     * 当你一个路由下面的 children 声明的路由大于1个时，自动会变成嵌套的模式--如组件页面
     */
    private Boolean alwaysShow;

    /**
     * 其他元素
     */
    private MetaVo meta;

    /**
     * 子路由
     */
    private List<RouterVo> children;

//    public String getName()
//    {
//        return name;
//    }
//
//    public void setName(String name)
//    {
//        this.name = name;
//    }
//
//    public String getPath()
//    {
//        return path;
//    }
//
//    public void setPath(String path)
//    {
//        this.path = path;
//    }
//
//    public boolean getHidden()
//    {
//        return hidden;
//    }
//
//    public void setHidden(boolean hidden)
//    {
//        this.hidden = hidden;
//    }
//
//    public String getRedirect()
//    {
//        return redirect;
//    }
//
//    public void setRedirect(String redirect)
//    {
//        this.redirect = redirect;
//    }
//
//    public String getComponent()
//    {
//        return component;
//    }
//
//    public void setComponent(String component)
//    {
//        this.component = component;
//    }
//
//    public String getQuery()
//    {
//        return query;
//    }
//
//    public void setQuery(String query)
//    {
//        this.query = query;
//    }
//
//    public Boolean getAlwaysShow()
//    {
//        return alwaysShow;
//    }
//
//    public void setAlwaysShow(Boolean alwaysShow)
//    {
//        this.alwaysShow = alwaysShow;
//    }
//
//    public MetaVo getMeta()
//    {
//        return meta;
//    }
//
//    public void setMeta(MetaVo meta)
//    {
//        this.meta = meta;
//    }
//
//    public List<RouterVo> getChildren()
//    {
//        return children;
//    }
//
//    public void setChildren(List<RouterVo> children)
//    {
//        this.children = children;
//    }
}
