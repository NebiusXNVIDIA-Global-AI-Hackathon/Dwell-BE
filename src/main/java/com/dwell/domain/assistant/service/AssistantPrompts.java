package com.dwell.domain.assistant.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/*
Deaver 시스템 프롬프트 + 응답 형식
- 모델은 답변 본문을 먼저 쓰고, 마지막에 META_MARKER 다음 줄에 JSON 한 줄(cards, quickReplies, action)을 붙임
- 서버는 본문만 토큰으로 스트리밍하고, JSON은 검증해서 card / action / quick_replies 이벤트로 보냄
- 법률 근거는 LEGAL_SOURCES 안의 id만 인용 가능 (서버가 목록에 없는 인용은 버림)
  TODO: legal_chunk(RAG) 데이터가 들어오면 검색 결과로 교체
 */
final class AssistantPrompts {

    static final String META_MARKER = "###META###";

    // 인용 가능한 법률 근거 (id -> [label, url]) — URL은 원문 조항이 아니라 공식 사이트 루트인 것도 있음, RAG 연결 시 교체
    static final Map<String, String[]> LEGAL_SOURCES = new LinkedHashMap<>();

    static {
        LEGAL_SOURCES.put("HMC_27_2005", new String[]{"NYC Housing Maintenance Code §27-2005 (owner's duty to keep in good repair)",
                "https://codelibrary.amlegal.com/codes/newyorkcity/latest/overview"});
        LEGAL_SOURCES.put("HMC_27_2018", new String[]{"NYC Housing Maintenance Code §27-2018 (rodents, insects and other pests)",
                "https://codelibrary.amlegal.com/codes/newyorkcity/latest/overview"});
        LEGAL_SOURCES.put("HMC_27_2029", new String[]{"NYC Housing Maintenance Code §27-2029 (heat)",
                "https://www.nyc.gov/site/hpd/services-and-information/heat-and-hot-water-information.page"});
        LEGAL_SOURCES.put("HMC_27_2031", new String[]{"NYC Housing Maintenance Code §27-2031 (hot water)",
                "https://www.nyc.gov/site/hpd/services-and-information/heat-and-hot-water-information.page"});
        LEGAL_SOURCES.put("LL_55_2018", new String[]{"NYC Local Law 55 of 2018 (mold and pest allergens)",
                "https://www.nyc.gov/site/hpd/index.page"});
        LEGAL_SOURCES.put("RPL_235_B", new String[]{"NY Real Property Law §235-b (warranty of habitability)",
                "https://www.nysenate.gov/legislation/laws/RPP/235-B"});
        LEGAL_SOURCES.put("NYC_311", new String[]{"NYC 311 — report a housing condition to HPD",
                "https://portal.311.nyc.gov/"});
    }

    private AssistantPrompts() {
    }

    // 채팅 시스템 프롬프트
    static String chatSystemPrompt(String userName, String preferredLanguage, String caseContext, String inspectionContext, String today) {
        String sources = LEGAL_SOURCES.entrySet().stream()
                .map(e -> "- " + e.getKey() + ": " + e.getValue()[0])
                .collect(Collectors.joining("\n"));
        String caseBlock = caseContext != null
                ? "This chat is about the user's case:\n" + caseContext
                : inspectionContext != null
                ? "This is a general chat (no case is attached). The user just checked their unit after neighbors reported an issue in the building:\n"
                + inspectionContext + "\nHelp them judge whether it is a real problem, ask for a photo, and suggest starting a case when it is."
                : "This is a general chat (no case is attached).";

        return """
                You are Deaver, an AI tenant advocate for renters in New York City.
                Help the user understand their rights, their landlord's messages, and the next step for repairs.
                The user's name is %s. Their preferred language is "%s". Today is %s.

                %s

                Rules:
                - Reply in the same language the user wrote in. Keep it short and practical (under 120 words). Plain text, no markdown headings.
                - Never invent laws. When you state a legal rule, cite one of these sources by id in a LEGAL_RULE card. If none fits, say you are not sure and suggest 311 or a tenant lawyer.
                %s
                - You are not a lawyer; give general information, not legal advice.

                After your reply, write a new line with exactly %s and then ONE line of minified JSON:
                {"cards":[...],"quickReplies":[...],"action":null}
                Card types (use only when helpful, at most 2 cards):
                - {"type":"TRANSLATION","label":"ENGLISH MEANING","body":"..."} when the user writes in a non-English language and quotes English, or asks what an English message means
                - {"type":"LEGAL_RULE","label":"SHORT TITLE IN CAPS","body":"the rule in one or two lines","citationId":"<source id>"}
                - {"type":"MESSAGE_DRAFT","label":"Want to send this?","body":"<message to the landlord in English>"} when you write a notice or reply for the landlord
                - {"type":"CASE_PREFILL","label":"WE'LL FILL IN","body":"Issue: ...\\nLocation: ...","prefill":{"issueCategory":"<HEAT_HOT_WATER|WATER_FLOODING|PLUMBING|MOLD_MOISTURE|PESTS|ELECTRICITY|GAS|APPLIANCES|VENTILATION|SAFETY>","location":"<BATHROOM|KITCHEN|LIVING_ROOM|HALLWAY|BEDROOM|BALCONY|EXTERIOR|ENTIRE_UNIT|LAUNDRY_AREA|OTHER>","description":"..."}} only in a general chat when the user describes a repair problem
                quickReplies: 0-3 short follow-up questions the user may tap, in the user's language.
                action (or null):
                - {"type":"SEND_MESSAGE"} together with a MESSAGE_DRAFT card, only in a case chat
                - {"type":"CREATE_CASE"} together with a CASE_PREFILL card, only in a general chat
                - {"type":"APPLY_LANDLORD_REPLY","intent":"<REPAIR_CONFIRMED|SCHEDULING|REFUSED|BLAME_SHIFT|VAGUE>","scheduledStart":"<ISO-8601 date-time or null>","landlordText":"<the landlord's message>"} only in a case chat when the user shares a message they received from their landlord
                Write nothing after the JSON line.
                """.formatted(userName, preferredLanguage, today, caseBlock, sources, META_MARKER);
    }

    // 집주인 답장에 대한 회신 초안 프롬프트
    static String replyDraftSystemPrompt(String caseContext, String landlordText, String intent) {
        return """
                You write short, polite, factual text messages from a NYC tenant to their landlord, in English.
                Case:
                %s
                The landlord wrote: "%s" (classified as %s).
                Write the tenant's reply in at most 4 sentences. Confirm any repair date or ask for a specific date and time,
                and keep a written record tone. Output only the message text, no quotes, no explanation.
                """.formatted(caseContext, landlordText, intent);
    }
}
