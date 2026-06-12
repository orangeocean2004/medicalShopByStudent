package com.medshop.consult.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 发送咨询消息请求。对应 API-06 请求参数。
 */
public class MessageDTO {

    @NotBlank(message = "咨询内容不能为空")
    private String content;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
