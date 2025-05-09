package ourpkg.review;

import java.sql.Timestamp;

import lombok.Data;

/**
 * 用於回傳 ReviewReply（多輪留言）資料給前端的 DTO。
 * 包含留言者資訊、留言內容、圖片、時間等。
 */
@Data
public class ReviewReplyDTO {

    /** 留言 ID */
    private Integer replyId;

    /** 留言者 userId（可用於區分買家/賣家） */
    private Integer userId;

    /** 留言者名稱 */
    private String username;

    /** 留言文字內容 */
    private String content;

    /** 留言附圖（圖片連結 URL） */
    private String imageUrl;

    /** 建立時間 */
    private Timestamp createdAt;
}
