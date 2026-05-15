#!/usr/bin/env python3
# -*- coding: utf-8 -*-

from __future__ import annotations

import json
import re
from dataclasses import dataclass
from pathlib import Path

import mysql.connector


DB_CONFIG = {
    "host": "localhost",
    "port": 3306,
    "user": "root",
    "password": "123456",
    "database": "poetry_clean",
    "charset": "utf8mb4",
}

REPO_ROOT = Path(__file__).resolve().parents[1]
CHINESE_POETRY_ROOT = REPO_ROOT.parent / "chinese-poetry"


@dataclass(frozen=True)
class SourceSpec:
    label: str
    path: Path
    dynasty_names: tuple[str, ...]
    desc_fields: tuple[str, ...]


SOURCE_SPECS = (
    SourceSpec(
        label="tang_authors",
        path=CHINESE_POETRY_ROOT / "全唐诗" / "authors.tang.json",
        dynasty_names=("唐代", "唐", "唐朝", "初唐", "盛唐", "中唐", "晚唐"),
        desc_fields=("desc",),
    ),
    SourceSpec(
        label="song_authors_full",
        path=CHINESE_POETRY_ROOT / "全唐诗" / "authors.song.json",
        dynasty_names=("宋代", "宋", "宋朝", "北宋", "南宋", "两宋"),
        desc_fields=("desc",),
    ),
    SourceSpec(
        label="song_ci_authors",
        path=CHINESE_POETRY_ROOT / "宋词" / "author.song.json",
        dynasty_names=("宋代", "宋", "宋朝", "北宋", "南宋", "两宋"),
        desc_fields=("desc", "description", "short_description"),
    ),
    SourceSpec(
        label="nantang_authors",
        path=CHINESE_POETRY_ROOT / "五代诗词" / "nantang" / "authors.json",
        dynasty_names=("五代", "五代十国", "五代诗词"),
        desc_fields=("desc",),
    ),
)


TRADITIONAL_CHAR_MAP = str.maketrans(
    {
        "萬": "万",
        "丘": "丘",
        "東": "东",
        "絲": "丝",
        "嚴": "严",
        "喬": "乔",
        "于": "于",
        "雲": "云",
        "亞": "亚",
        "劉": "刘",
        "衛": "卫",
        "吳": "吴",
        "周": "周",
        "唐": "唐",
        "啟": "启",
        "喻": "喻",
        "國": "国",
        "圓": "圆",
        "塵": "尘",
        "夢": "梦",
        "學": "学",
        "寧": "宁",
        "寶": "宝",
        "實": "实",
        "寬": "宽",
        "將": "将",
        "專": "专",
        "尋": "寻",
        "對": "对",
        "導": "导",
        "爾": "尔",
        "巖": "岩",
        "崙": "仑",
        "嶠": "峤",
        "嶽": "岳",
        "廣": "广",
        "張": "张",
        "彥": "彦",
        "後": "后",
        "徵": "征",
        "德": "德",
        "憲": "宪",
        "懷": "怀",
        "應": "应",
        "懿": "懿",
        "戰": "战",
        "戲": "戏",
        "戶": "户",
        "拱": "拱",
        "挹": "挹",
        "揚": "扬",
        "搖": "摇",
        "數": "数",
        "文": "文",
        "斂": "敛",
        "時": "时",
        "晉": "晋",
        "曉": "晓",
        "書": "书",
        "會": "会",
        "東": "东",
        "來": "来",
        "楊": "杨",
        "棄": "弃",
        "業": "业",
        "榮": "荣",
        "構": "构",
        "歐": "欧",
        "歷": "历",
        "歸": "归",
        "歲": "岁",
        "殷": "殷",
        "氣": "气",
        "漢": "汉",
        "湯": "汤",
        "滄": "沧",
        "潁": "颍",
        "澤": "泽",
        "濤": "涛",
        "瀛": "瀛",
        "燈": "灯",
        "營": "营",
        "爐": "炉",
        "爲": "为",
        "無": "无",
        "煥": "焕",
        "煒": "炜",
        "爭": "争",
        "狀": "状",
        "獨": "独",
        "珮": "佩",
        "瑩": "莹",
        "璉": "琏",
        "環": "环",
        "甌": "瓯",
        "當": "当",
        "盧": "卢",
        "眾": "众",
        "睿": "睿",
        "矯": "矫",
        "硯": "砚",
        "禮": "礼",
        "禪": "禅",
        "離": "离",
        "種": "种",
        "穎": "颖",
        "竇": "窦",
        "筆": "笔",
        "簡": "简",
        "紀": "纪",
        "紅": "红",
        "納": "纳",
        "純": "纯",
        "紹": "绍",
        "終": "终",
        "絳": "绛",
        "綸": "纶",
        "維": "维",
        "緯": "纬",
        "縣": "县",
        "繆": "缪",
        "羅": "罗",
        "聞": "闻",
        "肅": "肃",
        "脩": "修",
        "臺": "台",
        "興": "兴",
        "艤": "舣",
        "艱": "艰",
        "蘇": "苏",
        "處": "处",
        "葉": "叶",
        "薛": "薛",
        "藍": "蓝",
        "虞": "虞",
        "補": "补",
        "袁": "袁",
        "裴": "裴",
        "褒": "褒",
        "觀": "观",
        "覺": "觉",
        "詠": "咏",
        "詩": "诗",
        "誠": "诚",
        "說": "说",
        "課": "课",
        "諒": "谅",
        "諸": "诸",
        "謝": "谢",
        "謙": "谦",
        "譙": "谯",
        "譚": "谭",
        "豐": "丰",
        "貞": "贞",
        "賀": "贺",
        "賈": "贾",
        "趙": "赵",
        "軌": "轨",
        "軾": "轼",
        "輔": "辅",
        "輝": "辉",
        "遜": "逊",
        "遠": "远",
        "適": "适",
        "遼": "辽",
        "遊": "游",
        "達": "达",
        "遺": "遗",
        "鄆": "郓",
        "鄒": "邹",
        "鄭": "郑",
        "醇": "醇",
        "釋": "释",
        "鈍": "钝",
        "鉉": "铉",
        "銀": "银",
        "銘": "铭",
        "錢": "钱",
        "鍊": "炼",
        "鍾": "钟",
        "鎮": "镇",
        "鏐": "镠",
        "鐸": "铎",
        "鑄": "铸",
        "長": "长",
        "門": "门",
        "開": "开",
        "閑": "闲",
        "閎": "闳",
        "閔": "闵",
        "閣": "阁",
        "閩": "闽",
        "關": "关",
        "闐": "阗",
        "陽": "阳",
        "隂": "阴",
        "隨": "随",
        "雙": "双",
        "雋": "隽",
        "雖": "虽",
        "靈": "灵",
        "靜": "静",
        "韓": "韩",
        "項": "项",
        "順": "顺",
        "顏": "颜",
        "顥": "颢",
        "顯": "显",
        "颯": "飒",
        "馮": "冯",
        "駿": "骏",
        "騫": "骞",
        "驗": "验",
        "體": "体",
        "高": "高",
        "鮑": "鲍",
        "鴻": "鸿",
        "鶴": "鹤",
        "麥": "麦",
        "黃": "黄",
        "龔": "龚",
    }
)


ALIAS_MAP = {
    "太宗皇帝": "李世民",
    "高宗皇帝": "李治",
    "中宗皇帝": "李显",
    "睿宗皇帝": "李旦",
    "明皇帝": "李隆基",
    "肃宗皇帝": "李亨",
    "德宗皇帝": "李适",
    "文宗皇帝": "李昂",
    "宣宗皇帝": "李忱",
    "昭宗皇帝": "李晔",
    "宋太祖": "赵匡胤",
    "宋太宗": "赵炅",
    "宋真宗": "赵恒",
    "宋仁宗": "赵祯",
    "宋神宗": "赵顼",
    "宋徽宗": "赵佶",
    "宋高宗": "赵构",
    "幸夤逊": "幸夤逊",
}


def normalize_name(name: str) -> str:
    if not name:
        return ""
    value = re.sub(r"\s+", "", name)
    value = (
        value.replace("\\n", "")
        .replace("\n", "")
        .replace("\u3000", "")
        .replace("·", "")
        .replace("・", "")
        .strip()
    )
    return value


def traditional_to_simplified(name: str) -> str:
    return name.translate(TRADITIONAL_CHAR_MAP)


def build_name_candidates(name: str) -> list[str]:
    normalized = normalize_name(name)
    if not normalized:
        return []

    candidates: list[str] = []
    for value in (
        normalized,
        traditional_to_simplified(normalized),
        ALIAS_MAP.get(normalized, ""),
        ALIAS_MAP.get(traditional_to_simplified(normalized), ""),
    ):
        value = normalize_name(value)
        if value and value not in candidates:
            candidates.append(value)
    return candidates


def choose_desc(item: dict, desc_fields: tuple[str, ...]) -> str:
    for field in desc_fields:
        value = str(item.get(field, "") or "").strip()
        if value:
            return value
    return ""


def load_source_entries(spec: SourceSpec) -> dict[str, tuple[str, str]]:
    if not spec.path.exists():
        print(f"[WARN] missing source: {spec.path}")
        return {}

    data = json.loads(spec.path.read_text(encoding="utf-8"))
    merged: dict[str, tuple[str, str]] = {}

    for item in data:
        if not isinstance(item, dict):
            continue

        name = str(item.get("name", "") or "").strip()
        desc = choose_desc(item, spec.desc_fields)
        if not name or not desc:
            continue

        source_id = str(item.get("id", "") or spec.label).strip() or spec.label
        for candidate in build_name_candidates(name):
            existing = merged.get(candidate)
            if existing is None or len(desc) > len(existing[0]):
                merged[candidate] = (desc, source_id)

    print(f"[OK] {spec.label}: loaded {len(merged)} usable author intros")
    return merged


def merge_sources() -> dict[str, dict[str, tuple[str, str]]]:
    merged: dict[str, dict[str, tuple[str, str]]] = {}

    for spec in SOURCE_SPECS:
        entries = load_source_entries(spec)
        if not entries:
            continue

        dynasty_key = spec.dynasty_names[0]
        bucket = merged.setdefault(dynasty_key, {})
        for name, payload in entries.items():
            existing = bucket.get(name)
            if existing is None or len(payload[0]) > len(existing[0]):
                bucket[name] = payload

    return merged


def fetch_db_authors(conn, dynasty_names: tuple[str, ...]) -> list[tuple[int, str]]:
    cursor = conn.cursor()
    placeholders = ", ".join(["%s"] * len(dynasty_names))
    sql = f"""
        SELECT author_id, canonical_name
        FROM author
        WHERE dynasty_name IN ({placeholders})
          AND intro_text IS NULL
    """
    cursor.execute(sql, dynasty_names)
    rows = [(int(author_id), canonical_name) for author_id, canonical_name in cursor.fetchall()]
    cursor.close()
    return rows


def update_author_intro(conn, author_id: int, intro_text: str, source_id: str) -> int:
    cursor = conn.cursor()
    sql = """
        UPDATE author
        SET intro_text = %s,
            source_id = %s
        WHERE author_id = %s
          AND intro_text IS NULL
    """
    cursor.execute(sql, (intro_text, source_id, author_id))
    changed = cursor.rowcount
    cursor.close()
    return changed


def import_for_spec(conn, spec: SourceSpec, entries: dict[str, tuple[str, str]]) -> int:
    db_authors = fetch_db_authors(conn, spec.dynasty_names)
    updated = 0
    matched_names = 0

    for author_id, canonical_name in db_authors:
        matched_payload = None
        for candidate in build_name_candidates(canonical_name):
            payload = entries.get(candidate)
            if payload:
                matched_payload = payload
                break

        if matched_payload is None:
            continue

        intro_text, source_id = matched_payload
        matched_names += 1
        updated += update_author_intro(conn, author_id, intro_text, source_id)

    print(
        f"[OK] {spec.label}: matched {matched_names} authors, updated {updated} rows "
        f"for dynasties {', '.join(spec.dynasty_names)}"
    )
    return updated


def show_stats(conn) -> None:
    cursor = conn.cursor()
    cursor.execute(
        """
        SELECT
            COUNT(*) AS total_authors,
            SUM(CASE WHEN intro_text IS NOT NULL THEN 1 ELSE 0 END) AS with_intro,
            SUM(CASE WHEN intro_text IS NULL THEN 1 ELSE 0 END) AS without_intro
        FROM author
        """
    )
    total_authors, with_intro, without_intro = cursor.fetchone()
    print("\n[STATS]")
    print(f"total_authors={total_authors}")
    print(f"with_intro={with_intro}")
    print(f"without_intro={without_intro}")

    cursor.execute(
        """
        SELECT dynasty_name, COUNT(*) AS total,
               SUM(CASE WHEN intro_text IS NOT NULL THEN 1 ELSE 0 END) AS with_intro
        FROM author
        GROUP BY dynasty_name
        ORDER BY total DESC
        LIMIT 12
        """
    )
    for dynasty_name, total, with_intro in cursor.fetchall():
        print(f"{dynasty_name}\ttotal={total}\twith_intro={with_intro}")
    cursor.close()


def main() -> None:
    merged_sources = merge_sources()
    if not merged_sources:
        raise SystemExit("No usable source data found in chinese-poetry.")

    conn = mysql.connector.connect(**DB_CONFIG)
    try:
        total_updated = 0
        for spec in SOURCE_SPECS:
            entries = merged_sources.get(spec.dynasty_names[0], {})
            if not entries:
                continue
            total_updated += import_for_spec(conn, spec, entries)
        conn.commit()
        print(f"\n[OK] total updated author intros: {total_updated}")
        show_stats(conn)
    finally:
        conn.close()


if __name__ == "__main__":
    main()
