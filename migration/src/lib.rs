pub use sea_orm_migration::prelude::*;

mod m20240216_072943_create_user_table;
mod m20240216_102222_create_image_table;
mod m20240216_114539_create_user_image_table;
mod m20240216_120257_create_folder_table;
mod m20240216_122843_create_share_table;
mod m20240302_055459_create_trade_table;

pub struct Migrator;

#[async_trait::async_trait]
impl MigratorTrait for Migrator {
    fn migrations() -> Vec<Box<dyn MigrationTrait>> {
        vec![
            Box::new(m20240216_072943_create_user_table::Migration),
            Box::new(m20240216_102222_create_image_table::Migration),
            Box::new(m20240216_114539_create_user_image_table::Migration),
            Box::new(m20240216_120257_create_folder_table::Migration),
            Box::new(m20240216_122843_create_share_table::Migration),
            Box::new(m20240302_055459_create_trade_table::Migration)
        ]
    }
}
