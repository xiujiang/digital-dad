package com.digitaldad.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 语音转写配额响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpeechQuotaResponse {

    /** 剩余秒数 */
    private int remainingSeconds;

    /** 累计已使用秒数 */
    private int totalUsedSeconds;
}
