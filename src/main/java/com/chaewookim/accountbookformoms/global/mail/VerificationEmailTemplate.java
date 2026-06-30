package com.chaewookim.accountbookformoms.global.mail;

import com.chaewookim.accountbookformoms.domain.user.enums.VerificationType;

public final class VerificationEmailTemplate {

    public static final String SENDER_NAME = "Joint Living";

    private VerificationEmailTemplate() {
    }

    public static String getSubject(VerificationType type) {
        return type == VerificationType.SIGNUP
                ? "[Joint Living] 회원가입 인증번호"
                : "[Joint Living] 비밀번호 재설정 인증번호";
    }

    public static String buildPlainText(String code, VerificationType type) {
        String title = type == VerificationType.SIGNUP ? "회원가입" : "비밀번호 재설정";
        return """
                Joint Living %s 인증번호

                인증번호: %s

                인증번호는 3분간 유효합니다.
                본인이 요청하지 않았다면 이 메일을 무시해 주세요.
                """.formatted(title, code).strip();
    }

    public static String buildHtml(String code, VerificationType type) {
        boolean isSignup = type == VerificationType.SIGNUP;
        String title = isSignup ? "회원가입 인증번호" : "비밀번호 재설정 인증번호";
        String description = isSignup
                ? "아래 인증번호를 입력하여 이메일 인증을 완료해 주세요."
                : "아래 인증번호를 입력하여 비밀번호 재설정을 진행해 주세요.";

        return """
                <!DOCTYPE html>
                <html lang="ko">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>%s</title>
                </head>
                <body style="margin:0;padding:0;background-color:#f1f5f9;font-family:'Apple SD Gothic Neo','Malgun Gothic','Noto Sans KR',sans-serif;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background-color:#f1f5f9;padding:40px 20px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:480px;background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(15,23,42,0.08);">
                          <tr>
                            <td style="background:#0f172a;padding:32px 24px;text-align:center;">
                              <h1 style="margin:0;color:#ffffff;font-size:24px;font-weight:800;letter-spacing:-0.5px;">Joint Living</h1>
                              <p style="margin:8px 0 0;color:rgba(255,255,255,0.6);font-size:14px;font-weight:500;">함께하는 생활, 스마트한 가계부</p>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:32px 24px;">
                              <h2 style="margin:0 0 8px;color:#1e293b;font-size:20px;font-weight:700;">%s</h2>
                              <p style="margin:0 0 24px;color:#64748b;font-size:14px;line-height:1.6;">%s</p>
                              <div style="background:#f8fafc;border:2px dashed #c7d2fe;border-radius:12px;padding:24px;text-align:center;margin-bottom:24px;">
                                <span style="display:block;color:#64748b;font-size:12px;margin-bottom:8px;letter-spacing:0.5px;">인증번호</span>
                                <span style="display:block;color:#4f46e5;font-size:36px;font-weight:700;letter-spacing:8px;font-family:Consolas,'Courier New',monospace;">%s</span>
                              </div>
                              <p style="margin:0;color:#94a3b8;font-size:12px;line-height:1.6;text-align:center;">
                                인증번호는 <strong style="color:#64748b;">3분</strong>간 유효합니다.<br>
                                본인이 요청하지 않았다면 이 메일을 무시해 주세요.
                              </p>
                            </td>
                          </tr>
                          <tr>
                            <td style="background:#f8fafc;padding:20px 24px;text-align:center;border-top:1px solid #e2e8f0;">
                              <p style="margin:0;color:#94a3b8;font-size:11px;">&copy; Joint Living. All rights reserved.</p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(title, title, description, code);
    }
}
