use std::cmp::Ordering;

use chrono::{DateTime, TimeZone, Utc};
use serde::{Deserialize, Serialize};

#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct LevelInfo {
    level: Level,
    start: DateTime<Utc>,
    end: DateTime<Utc>,
}

impl LevelInfo {
    pub fn is_valid(&self) -> bool {
        let now = Utc::now();
        self.start < now && self.end > now
    }
    
    pub fn get_free_level() -> Self {
        LevelInfo {
            level: Level::Free,
            start: Utc.timestamp_opt(0, 0).unwrap(),
            end: Utc.timestamp_opt(0, 0).unwrap(),
        }
    }
}

impl Eq for LevelInfo {}

impl PartialEq<Self> for LevelInfo {
    fn eq(&self, other: &Self) -> bool {
        self.level == other.level
    }
}

impl PartialOrd<Self> for LevelInfo {
    fn partial_cmp(&self, other: &Self) -> Option<Ordering> {
        if !self.is_valid() || !other.is_valid() {
            return None;
        }
        Some(self.cmp(other))
    }
}

impl Ord for LevelInfo {
    fn cmp(&self, other: &Self) -> Ordering {
        self.level.cmp(&other.level)
    }
}


pub fn remove_invalid_levels(levels: Vec<LevelInfo>) -> Vec<LevelInfo> {
    levels.into_iter().filter(|level| level.is_valid()).collect()
}

#[derive(Clone, Debug, Serialize, Deserialize, Ord, PartialOrd, Eq, PartialEq)]
pub enum Level {
    Free = 0,
    Started,
    Advanced,
    Professional,
}

impl From<u8> for Level {
    fn from(value: u8) -> Self {
        match value {
            0 => Level::Free,
            1 => Level::Started,
            2 => Level::Advanced,
            3 => Level::Professional,
            _ => Level::Free,
        }
    }
}


impl TryFrom<Vec<i64>> for LevelInfo {
    type Error = std::io::Error;

    fn try_from(value: Vec<i64>) -> Result<Self, Self::Error> {
        if value.len() != 3 {
            return Err(std::io::Error::new(std::io::ErrorKind::InvalidData, "Invalid data length"));
        }
        if value[0] > 3 || value[0] < 0 {
            return Err(std::io::Error::new(std::io::ErrorKind::InvalidData, "Invalid level"));
        }

        Ok(LevelInfo {
            level: Level::from(value[0] as u8),
            start: Utc.timestamp_opt(value[1], 0).unwrap(),
            end: Utc.timestamp_opt(value[2], 0).unwrap(),
        })
    }
}

impl From<LevelInfo> for Vec<i64> {
    fn from(value: LevelInfo) -> Self {
        vec![
            value.level as i64,
            value.start.timestamp(),
            value.end.timestamp(),
        ]
    }
}

#[test]
fn test_level_serialize() {
    let level_str = r#"{"level":"Started","start":"2024-02-20T07:57:52Z","end":"2024-02-27T07:57:52Z"}"#;
    let level_info: LevelInfo = serde_json::from_str(level_str).unwrap();

    assert_eq!(serde_json::to_string(&level_info).unwrap(), level_str);

    let level_array_str = r#"[1,1708415872,1709020672]"#;
    let level_array: Vec<i64> = serde_json::from_str(level_array_str).unwrap();
    let level_info2 = LevelInfo::try_from(level_array).unwrap();

    println!("{}", serde_json::to_string(&level_info2).unwrap());

    assert_eq!(level_info2.level as u8, level_info.level as u8);
    assert_eq!(level_info2.start, level_info.start);
    assert_eq!(level_info2.end, level_info.end);
}