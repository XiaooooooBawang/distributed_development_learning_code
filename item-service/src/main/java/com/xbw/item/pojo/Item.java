package com.xbw.item.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;

import java.util.Date;

@Data
@TableName("tb_item")
public class Item {
    /*
     * 因为canal不依赖与mybatis，所以需要额外标记实体类与数据库表做映射
     *
     * 用@Id、@Column、@Transient(最常用这3种)等注解完成Item与数据库表字段的映射
     * @Id        标记表中的id字段，是springframework中的注解，导入时要注意
     * @Column(。。= “。。”)    标记表中与属性名不一致的字段，一般用不上，因为有驼峰转换，都是能一致的
     * @Transient  标记不属于数据库表中的字段，比如这里的stock和sold,也是springframework中的注解，导入时要注意
     */
    @TableId(type = IdType.AUTO)
    @Id  //标记表中的id字段
    private Long id;//商品id
    private String name;//商品名称
    private String title;//商品标题
    private Long price;//价格（分）
    private String image;//商品图片
    private String category;//分类名称
    private String brand;//品牌名称
    private String spec;//规格
    private Integer status;//商品状态 1-正常，2-下架
    private Date createTime;//创建时间
    private Date updateTime;//更新时间
    @TableField(exist = false)
    @Transient
    private Integer stock;
    @TableField(exist = false)
    @Transient
    private Integer sold;
}
