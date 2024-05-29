use std::cmp::Ordering;
use std::fmt::{Display, Formatter};

use chrono::{Datelike, Months, NaiveDate, Utc};
use sea_orm::{ActiveModelTrait, EntityTrait, IntoActiveModel, Set};
use serde::{Deserialize, Serialize};

use crate::model::prelude::User;

#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct LevelInfo {
    pub level: Level,
    start: NaiveDate,
    end: NaiveDate,
}

impl LevelInfo {
    pub fn is_valid(&self) -> bool {
        let now = Utc::now().date_naive();
        self.start < now && self.end > now
    }

    pub fn get_free_level() -> Self {
        LevelInfo {
            level: Level::Free,
            start: NaiveDate::MIN,
            end: NaiveDate::MAX,
        }
    }

    pub fn remove_invalid_levels(levels: Vec<Self>) -> Vec<Self> {
        levels.into_iter().filter(|level| level.is_valid()).collect()
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
            start: NaiveDate::from_num_days_from_ce_opt(value[1] as i32).unwrap(),
            end: NaiveDate::from_num_days_from_ce_opt(value[2] as i32).unwrap(),
        })
    }
}

impl From<LevelInfo> for Vec<i64> {
    fn from(value: LevelInfo) -> Self {
        vec![
            value.level as i64,
            value.start.num_days_from_ce() as i64,
            value.end.num_days_from_ce() as i64,
        ]
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


#[derive(Clone, Debug, Serialize, Deserialize, Ord, PartialOrd, Eq, PartialEq)]
pub enum Level {
    Free = 0,
    Started,
    Advanced,
    Professional,
}

impl Level {
    pub fn get_price(&self) -> i32 {
        match self {
            Level::Free => 0,
            Level::Started => 3000,
            Level::Advanced => 5000,
            Level::Professional => 15000,
        }
    }

    pub fn get_max_share_level(&self) -> ShareLevel {
        match self {
            Level::Free => ShareLevel::Watermarked,
            Level::Started => ShareLevel::Compressed,
            Level::Advanced => ShareLevel::Compressed,
            Level::Professional => ShareLevel::Original,
        }
    }
}

impl Display for Level {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        match self {
            Level::Free => write!(f, "免费"),
            Level::Started => write!(f, "入门"),
            Level::Advanced => write!(f, "进阶"),
            Level::Professional => write!(f, "专业"),
        }
    }
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


#[derive(Clone, Debug, Serialize, Deserialize, Ord, PartialOrd, Eq, PartialEq)]
pub enum ShareLevel {
    Original = 0,
    Compressed,
    Watermarked,
}

impl TryFrom<i16> for ShareLevel {
    type Error = std::io::Error;

    fn try_from(value: i16) -> Result<Self, Self::Error> {
        match value {
            0 => Ok(ShareLevel::Original),
            1 => Ok(ShareLevel::Compressed),
            2 => Ok(ShareLevel::Watermarked),
            _ => Err(std::io::Error::new(std::io::ErrorKind::InvalidData, "Invalid share level")),
        }
    }
}


pub async fn add_level_to_user(user_id: i64, level: Level, period: i16, start_time: NaiveDate) -> Result<(), String> {
    let level_info: Vec<i64> = LevelInfo {
        level,
        start: start_time,
        end: start_time.checked_add_months(Months::new(period as u32)).unwrap(),
    }.into();
    let user = User::find_by_id(user_id).one(&*crate::DATABASE).await.unwrap().ok_or("Cannot fond user".to_string())?;
    let mut levels = user.level.clone();
    levels.push(serde_json::to_string(&level_info).unwrap());

    let mut user = user.into_active_model();
    user.level = Set(levels);
    user.save(&*crate::DATABASE).await.unwrap();

    Ok(())
}

#[test]
fn test_level_serialize() {
    let level_str = r#"{"level":"Started","start":"2024-02-20","end":"2025-02-27"}"#;
    let level_info: LevelInfo = serde_json::from_str(level_str).unwrap();

    assert_eq!(serde_json::to_string(&level_info).unwrap(), level_str);

    let level_array_str = r#"[1,738936,739309]"#;
    let level_array: Vec<i64> = serde_json::from_str(level_array_str).unwrap();
    let level_info2 = LevelInfo::try_from(level_array).unwrap();

    println!("level: {}", serde_json::to_string::<Vec<i64>>(&level_info2.clone().into()).unwrap());

    assert_eq!(level_info2, level_info);
}