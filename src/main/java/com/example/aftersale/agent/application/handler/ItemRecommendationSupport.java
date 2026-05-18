package com.example.aftersale.agent.application.handler;

import com.example.aftersale.agent.application.workspace.OrderItemFact;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class ItemRecommendationSupport {

    private ItemRecommendationSupport() {
    }

    static String returnRecommendation(SubtaskExecutionContext context) {
        return recommendation(context, true);
    }

    static String exchangeRecommendation(SubtaskExecutionContext context) {
        return recommendation(context, false);
    }

    private static String recommendation(SubtaskExecutionContext context, boolean returnIntent) {
        List<OrderItemFact> items = context.workspace().orderFacts().stream()
                .filter(orderFact -> context.ticket().getOrderId().equals(orderFact.orderId()))
                .flatMap(orderFact -> orderFact.orderItems().stream())
                .toList();
        if (items.isEmpty()) {
            return "Item-level recommendation: no orderItems were available from get_order_by_id; "
                    + "use policy evidence only and do not execute real after-sale action.";
        }

        MatchResult match = selectItem(items, matchText(context));
        OrderItemFact item = match.item();
        String action = returnIntent ? "return" : "exchange";
        boolean supported = returnIntent ? item.supportReturn() : item.supportExchange();
        String recommendation = item.specialItem() || !supported
                ? "Do not recommend direct " + action + "; route to policy/manual review boundary."
                : "Recommend " + action + " consultation for this item only; no real " + action
                        + " action is executed.";
        String reason = reason(item, supported, match.fallback(), returnIntent);

        return "Item-level " + action + " recommendation: "
                + "orderItemId=" + item.orderItemId()
                + ", productId=" + item.productId()
                + ", productName=" + item.productName()
                + ", category=" + item.category()
                + ", supportReturn=" + item.supportReturn()
                + ", supportExchange=" + item.supportExchange()
                + ", isSpecialItem=" + item.specialItem()
                + ", recommendation=" + recommendation
                + ", reason=" + reason;
    }

    private static MatchResult selectItem(List<OrderItemFact> items, String text) {
        Optional<OrderItemFact> byName = items.stream()
                .filter(item -> containsNormalized(text, item.productName()))
                .findFirst();
        if (byName.isPresent()) {
            return new MatchResult(byName.get(), false);
        }
        Optional<OrderItemFact> byCategory = items.stream()
                .filter(item -> containsNormalized(text, item.category()))
                .findFirst();
        if (byCategory.isPresent()) {
            return new MatchResult(byCategory.get(), false);
        }
        Optional<OrderItemFact> byClothingWord = items.stream()
                .filter(item -> hasClothingWord(text) && clothingCategory(item.category()))
                .findFirst();
        if (byClothingWord.isPresent()) {
            return new MatchResult(byClothingWord.get(), false);
        }
        return new MatchResult(items.get(0), true);
    }

    private static String reason(OrderItemFact item, boolean supported, boolean fallback, boolean returnIntent) {
        StringBuilder reason = new StringBuilder();
        if (fallback) {
            reason.append("fallback item selected because target/message did not match productName or category; ");
        } else {
            reason.append("matched item from subtask target/message; ");
        }
        if (item.specialItem()) {
            reason.append("special item restriction applies; ");
        }
        if (!supported) {
            reason.append(returnIntent ? "supportReturn=false" : "supportExchange=false");
        } else {
            reason.append(returnIntent ? "supportReturn=true" : "supportExchange=true");
            reason.append("; no special restriction found in current demo data");
        }
        return reason.toString();
    }

    private static String matchText(SubtaskExecutionContext context) {
        return normalize(context.subtask().target()
                + " "
                + context.subtask().userMessageFragment()
                + " "
                + context.ticket().getRawUserMessage());
    }

    private static boolean containsNormalized(String text, String candidate) {
        String normalizedCandidate = normalize(candidate);
        return !normalizedCandidate.isBlank()
                && !"n/a".equals(normalizedCandidate)
                && text.contains(normalizedCandidate);
    }

    private static boolean hasClothingWord(String text) {
        return text.contains("裙子")
                || text.contains("衣服")
                || text.contains("上衣")
                || text.contains("裤子")
                || text.contains("服装")
                || text.contains("尺码");
    }

    private static boolean clothingCategory(String category) {
        String normalized = normalize(category);
        return normalized.contains("服")
                || normalized.contains("衣")
                || normalized.contains("鞋")
                || normalized.contains("clothing")
                || normalized.contains("dress")
                || normalized.contains("pants");
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).trim();
    }

    private record MatchResult(OrderItemFact item, boolean fallback) {
    }
}
