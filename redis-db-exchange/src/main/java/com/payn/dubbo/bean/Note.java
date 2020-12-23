package com.payn.dubbo.bean;

import com.payn.redis.annation.RedisCache;
import com.payn.redis.annation.RedisFieldNotCache;
import com.payn.redis.annation.RedisQuery;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.sql.Blob;
import java.util.Date;

/**
 * 笔记实体类
 */
@Getter
@Setter
@RedisCache
public class Note implements Serializable {
	@RedisFieldNotCache
	private static final long serialVersionUID = 1L;
	@RedisFieldNotCache
	private static final String className = "Note";
	@RedisFieldNotCache
	private static final String primaryKey = "noteId";

	private int noteId;
	private String noteName;// 笔记名称
	@RedisQuery
	private String authorName;// 作者名称
	@RedisQuery
	private String fromUrl;// 文本来源
	private String content;// 文本内容
	private NoteBook noteBook;// 笔记本id
	private NoteBookGroup noteBookGroup;// 笔记本组
	@RedisQuery
	private Integer flag;// 放到BaseBean里面，反射获取不到field值
	@RedisQuery
	private Date createdate;
	@RedisFieldNotCache
	private Blob blobContent;

	public String toString() {
		return "输出值==>id=" + noteId + " 笔记本名称：" + noteName + "   文本来源：" + fromUrl + "  作者名称:" + authorName;
	}

}
