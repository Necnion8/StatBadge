package com.gmail.necnionch.myplugin.statbadge.bukkit.config;

public enum Lang {
    COMMAND_BADGE_GRANT_ALREADY("&c%2$s は称号 &6%1$s &cをすでに持っています"),
    COMMAND_BADGE_GRANT("&f%2$s に称号 &6%1$s &fを与えました"),
    COMMAND_BADGE_GRANT_ERROR("&c%2$s に称号 &6%1$s &cを付与できませんでした"),
    COMMAND_BADGE_REVOKE_ALREADY("&c%2$s は称号 &6%1$s &cを持っていません"),
    COMMAND_BADGE_REVOKE("&f%2$s の称号 &6%1$s &fを剥奪しました"),
    COMMAND_BADGE_REVOKE_ERROR("&c%2$s から称号 &6%1$s &cを剥奪できませんでした"),
    COMMAND_BADGE_LIST("&f%3$s が獲得している称号は &e%1$d &fあります &7(合計: %2$d)"),
    COMMAND_BADGE_LIST_ITEM_TITLE("&f称号 "),
    COMMAND_BADGE_LIST_ITEM_TITLE_COMPLETE("&a (達成)"),
    COMMAND_BADGE_LIST_ITEM_TITLE_NONE(""),
    COMMAND_BADGE_LIST_ITEM_STATS("&7統計:"),
    COMMAND_BADGE_LIST_ITEM_STATS_DATE("&7  期間: &f%1$s &7から &f%2$s"),
    COMMAND_BADGE_LIST_ITEM_STATS_VALUE("&7  目標値: &f%2$s  &7現在: &f%1$s &7(%3$.1f%%)"),
    COMMAND_BADGE_LIST_ITEM_STATS_VALUE_COMPLETED("&7  目標値: &f%2$s  &7現在: &a%1$s &7(&2%3$.1f%%&7)"),
    COMMAND_BADGE_LIST_ITEM_NAME("&7表示: "),
    COMMAND_BADGE_LIST_ITEM_DESCRIPTION("&7説明:"),
    COMMAND_BADGES_EMPTY("&c獲得した称号はまだありません"),
    COMMAND_PLAYER_ARGUMENT("&cプレイヤーを指定してください"),
    COMMAND_UNKNOWN_BADGE("&c称号 &6%1$s &cが見つかりません"),
    COMMAND_UNKNOWN_PLAYER("&cプレイヤーが見つかりません"),
    COMMAND_NO_LOADED_DATA("&cプラグインの準備ができていません"),
    UI_TODAY("今日"),
    UI_VALUE_SECONDS("%1$s秒"),
    UI_VALUE_MINUTES("%2$s分"),
    UI_VALUE_HOURS("%3$s時間"),
    UI_VALUE_DAYS("%4$s日%3$s時間"),
    UI_UNIT_KILL("%,dキル"),
    UI_UNIT_AMOUNT("%,d個"),
    UI_UNIT_COUNT("%,d回"),
    UI_UNIT_CHUNKS("%,dチャンク"),
    UI_BADGE_LIST_TITLE("称号一覧 (%1$d/%2$d) (達成率: &l%3$.0f%%&r)"),
    UI_BADGE_LIST_BACK_PAGE("&b前のページへ &7(%1$d/%2$d)"),
    UI_BADGE_LIST_NEXT_PAGE("&6次のページへ &7(%1$d/%2$d)"),
    // badgeId, badgeTitle, badgeName, badgeDesc, targetValue, value, startTime, completeTime, completePercentage
    UI_BADGE_LIST_ITEM_TITLE("&f称号 &6%3$s"),
    UI_BADGE_LIST_ITEM_DESCRIPTION("&7%4$s\n\n&f目標:  %5$s &7(%9$.1f%%)"),
    UI_BADGE_LIST_ITEM_TITLE_COMPLETED("&f称号 &6%3$s  &a(獲得)"),
    UI_BADGE_LIST_ITEM_DESCRIPTION_COMPLETED("&7%4$s\n\n&f目標数:  %5$s"),
    UI_BADGE_LIST_ITEM_TITLE_SELECTED("&f称号 &6%3$s  &e(表示中)"),
    UI_BADGE_LIST_ITEM_DESCRIPTION_SELECTED("&7%4$s\n\n&f目標数:  %5$s"),
    ;

    private final String defaultText;

    Lang(String defaultText) {
        this.defaultText = defaultText;
    }

    public String getDefaultText() {
        return defaultText;
    }

    public String getKey() {
        return name().replace("_", "-");
    }

}