# StatBadge
プレイヤーの行動回数や統計目標の達成で称号が与えられるプラグイン

## 前提
- Spigot 1.13 以上
- Java 17 以上
- [KyoriAdventureLib](https://github.com/Necnion8/KyoriAdventureLib) (Paper 1.16.5 以降なら不要)
- [MythicMobs](https://www.spigotmc.org/resources/5702) (optional)
- [Lands](https://www.spigotmc.org/resources/53313) (optional)
- [AFKPlus](https://www.spigotmc.org/resources/35065) (optional)
- [PlaceholderAPI](https://www.spigotmc.org/resources/6245) (optional)

## コマンドと権限
| コマンド                                    | サブコマンド / 説明                                           | 権限                          | デフォルト |
|-----------------------------------------|-------------------------------------------------------|:----------------------------|:-----:|
| /badges<br><sup>称号の一覧表示と表示する称号の選択</sup> |                                                       | statbadge.command.badges    |  YES  |
| /statbadge<br><sup>管理者用コマンド</sup>       |                                                       | statbadge.command.statbadge | OPのみ  |
| 〃                                       | list [player]<br><sup>プレイヤーの称号を表示</sup>               | 〃                           |   〃   |
| 〃                                       | grant (player) (badge)<br><sup>称号をプレイヤーに与えます</sup>    | 〃                           |   〃   |
| 〃                                       | revoke (player) (badge)<br><sup>称号をプレイヤーから剥奪します</sup> | 〃                           |   〃   |
| 〃                                       | reload<br><sup>設定ファイルの再読み込み</sup>                     | 〃                           |   〃   |

## プレースホルダ
#### プレイヤー
- 称号数 - `%statbadge_badge_count%` 
- 獲得済み称号数 - `%statbadge_badge_count_completed%`
- 獲得済み称号の割合 - `%statbadge_badge_progress%`
#### プレイヤーが選択した称号 と 特定の称号
- 称号のID / 名前 / タグ
  - `%statbadge_id%`
  - `%statbadge_id_<badge_id>%`
  - `%statbadge_name%` 
  - `%statbadge_name_<badge_id>%`
  - `%statbadge_title%`
  - `%statbadge_title_<badge_id>%`
- 称号の達成の有無と現在の進捗度
  - `%statbadge_completed%`
  - `%statbadge_completed_<badge_id>%`
  - `%statbadge_current_progress%`
  - `%statbadge_current_progress_<badge_id>%`
- 統計の現在の値 (生の値 / フォーマット済み)
  - `%statbadge_current_value_raw%`
  - `%statbadge_current_value_raw_<badge_id>%`
  - `%statbadge_current_value%`
  - `%statbadge_current_value_<badge_id>%`
- 統計の目標の値 (生の値 / フォーマット済み)
  - `%statbadge_target_value_raw%`
  - `%statbadge_target_value_raw_<badge_id>%`
  - `%statbadge_target_value%`
  - `%statbadge_target_value_<badge_id>%`
- 統計の日付 (統計の開始 / 達成)
  - `%statbadge_start_time`
  - `%statbadge_start_time_<badge_id>`
  - `%statbadge_complete_time%`
  - `%statbadge_complete_time_<badge_id>%`

## 設定
**デフォルトのプラグイン設定** [./plugins/StatBadge/config.yml](src%2Fmain%2Fresources%2Fbukkit-config.yml)<br>
**デフォルトの称号設定** [./plugins/StatBadge/badges.yml](src%2Fmain%2Fresources%2Fbadges.yml)

---
### 統計

**アクションタイプ: `entity_killed`**
エンティティを倒した数
```yml
(badge_id):
  stats:
    action: entity_killed
    entity: zombie
    value: 20  # 目標: ゾンビを20体倒す
```

**アクションタイプ: `entity_death`**
エンティティによって倒された数
```yml
(badge_id):
  stats:
    action: entity_death
    entity: zombie
    value: 20  # 目標: ゾンビに20回倒される
```

**アクションタイプ: `online_time`**
ログインした時間
```yml
(badge_id):
  stats:
    action: online_time
    value: 24  # 目標: ログイン時間が合計で24時間に達する
```

**アクションタイプ: `play_time`**
プレイした時間<br>
※ AFK時間を除外されます (AFKPlusプラグインの導入が必要です)
```yml
(badge_id):
  stats:
    action: play_time
    value: 12  # 目標: プレイ時間が合計で12時間に達する
```

---
### MythicMobs 統計

**アクションタイプ: `mythicmobs_killed`**
エンティティを倒した数
```yml
(badge_id):
  stats:
    action: mythicmobs_killed
    mob: SkeletonKing
    value: 10  # 目標: スケルトンキングを10体倒す
```

**アクションタイプ: `mythicmobs_death`**
エンティティによって倒された数
```yml
(badge_id):
  stats:
    action: mythicmobs_death
    mob: SkeletonKing
    value: 10  # 目標: スケルトンキングに10回倒される
```

---
### Lands 統計

**統計タイプ: `lands_chunks`**
Lands 所持チャンク数
```yml
(badge_id):
  stats:
    type: lands_chunk
    value: 20  # 目標: 20チャンクのLandsを所持
```

**アクションタイプ: `lands_war_wins`**
Lands 戦争 勝利数
```yml
(badge_id):
  stats:
    action: lands_war_wins
    value: 20  # 目標: 戦争に20回勝利
```

**アクションタイプ: `lands_war_loses`**
Lands 戦争 敗北数
```yml
(badge_id):
  stats:
    action: lands_war_loses
    value: 20  # 目標: 戦争に20回敗北
```


## API
**StatBadgeプラグインはプレイヤーの行動統計 [(PlayerActionStats)](src/main/java/com/gmail/necnionch/myplugin/statbadge/bukkit/stats/PlayerActionStats.java) や統計 [(PlayerStats)](src/main/java/com/gmail/necnionch/myplugin/statbadge/bukkit/stats/PlayerStats.java) を追加拡張することができます。**

[PlayerActionStats](src/main/java/com/gmail/necnionch/myplugin/statbadge/bukkit/stats/PlayerActionStats.java)は行動発生時にカウントする統計を作成できます。(StatBadgeプラグインがカウントおよび集計)<br>
外部プラグインが統計を管理している場合は[PlayerStats](src/main/java/com/gmail/necnionch/myplugin/statbadge/bukkit/stats/PlayerStats.java)を使用できます。(外部で集計した値を使用)<br>


#### Step 1: StatManager の取得
```java
// アクションタイプの作成
private final ActionType TEST_ACTION = new ActionType(this/* your plugin */, "test");

public void onEnable() {
    // 統計マネージャーを取得
    StatBadgePlugin statBadgePlugin = (StatBadgePlugin) getServer().getPluginManager().getPlugin("StatBadge");
    StatManager stats = statBadgePlugin.getStatManager();
    
    // アクションタイプの登録
    stats.addPlayerActionStatsProvider(TEST_ACTION, new PlayerActionStatsProvider(this) {
        @Override
        public PlayerActionStats create(UUID playerId, ConfigurationSection config, long targetValue) throws ConfigurationError {
            return new PlayerTestActionStats(playerId, TEST_ACTION, 0, targetValue);
        }
    });
}
```
#### Step 2: *coming soon*


## ライセンス
- [HikariCP](https://github.com/brettwooldridge/HikariCP) - [Apache License 2.0](https://github.com/brettwooldridge/HikariCP/blob/dev/LICENSE)
- [KyoriPowered/adventure](https://github.com/KyoriPowered/adventure) - [MIT License](https://github.com/KyoriPowered/adventure/blob/main/4/license.txt)
 