//! `SeaORM` Entity. Generated by sea-orm-codegen 0.12.6

use sea_orm::entity::prelude::*;
use serde::Serialize;
use crate::service::user::level::LevelInfo;

#[derive(Clone, Debug, PartialEq, DeriveEntityModel, Eq, Serialize)]
#[sea_orm(table_name = "user")]
pub struct Model {
    #[sea_orm(primary_key)]
    pub id: i64,
    #[sea_orm(unique)]
    pub username: String,
    #[serde(skip_serializing)]
    pub password: String,
    #[sea_orm(unique)]
    pub phone: String,
    pub email: Option<String>,
    pub available: bool,
    #[serde(skip_serializing)]
    pub level: Vec<String>,
    pub root: i64,
    pub create_time: DateTime,
    pub update_time: DateTime,
}

#[derive(Serialize)]
pub struct UserExtended {
    pub id: i64,
    pub username: String,
    pub phone: String,
    pub email: Option<String>,
    pub available: bool,
    pub level: LevelInfo,
    pub root: i64,
    pub used_space: f64,
    pub create_time: DateTime,
    pub update_time: DateTime,
}



#[derive(Copy, Clone, Debug, EnumIter, DeriveRelation)]
pub enum Relation {}

impl ActiveModelBehavior for ActiveModel {}
