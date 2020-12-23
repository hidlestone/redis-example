# 05-redis使用规范

本文档制定使用Redis规范和使用要求，制定规范命名和流程化的使用规则，从而降低后期维护、扩容、管理的成本。

### 一、名词释义
- Redis	可基于内存亦可持久化的日志型、Key-Value数据库。
- TableName	实体类名称,例如bi_menu表对应的实体类是BiMenu
- PrimaryKeyValue	某个表的主键值
- Column	实体类里面的字段名称。例如bi_menu表对应的实体类字段名称name
- ColumnValue	列的值
- index	字符串index代表此key是用来记录索引的
- sort	字符串sort代表此key是用来排序标记的

### 二、开发规则
#### 2.1、key 命名
- 1、存放表里面所有字段信息。TableName: PrimaryKeyValue: Column
- 2、存放表里面某一列的信息  TableName：Column：ColumnValue
- 3、存放表里面某一行object信息。TableName: PrimaryKeyValue
- 4、存放表里面的索引。TableName: index: Column
- 5、存放表里面的字段排序。TableName: sort: Column

```
bi_menu (菜单按钮)
是否主键	字段名	字段描述	数据类型	长度	可空	约束	缺省值	备注
是	id	主键	VARCHAR(36)	36				
否	name	名称	VARCHAR(200)	200	是		NULL	
否	sequence	序列	INT(11)	11	是		NULL	
否	description	描述	VARCHAR(200)	200	是		NULL	
否	logo	图标	VARCHAR(200)	200	是		NULL	
否	url	访问地址	VARCHAR(200)	200	是		NULL	点击访问地址
```

类型1(value就是具体的值)
```
BiMenu:1:id	1
BiMenu:1:name	系统应用
BiMenu:1:sequence	1
BiMenu:1:description	这是系统应用的按钮
BiMenu:1:logo	图片地址
BiMenu:1:url	http://localhost:8080/aa.do
```

类型2(value就是改行的主键值PrimaryKeyValue)
```
BiMenu:name:系统应用	1
BiMenu:url:http://www.baidu.com	1
```

类型3(value是改行数据的jason格式)
```
BiMenu:1	{
    "id": "1",
    "name": "系统应用",
    " sequence ": "1",
    " description ": "这是系统应用的按钮"
}
```

类型4(value是改行数据的主键值PrimaryKeyValue)
```
BiMenu:index:id	BiMenu:1
BiMenu:index:id	BiMenu:2
BiMenu:index:id	BiMenu:3
```

类型5(value是改行数据的主键值PrimaryKeyValue)
```
BiMenu:sort:sequence	BiMenu:1
BiMenu:sort:sequence	BiMenu:2
BiMenu:sort:sequence	BiMenu:3
```




