use sea_orm_migration::prelude::*;

#[derive(DeriveMigrationName)]
pub struct Migration;

#[async_trait::async_trait]
impl MigrationTrait for Migration {
    async fn up(&self, manager: &SchemaManager) -> Result<(), DbErr> {
        manager
            .create_table(
                Table::create()
                    .table(Trade::Table)
                    .if_not_exists()
                    .col(
                        ColumnDef::new(Trade::Id)
                            .string()
                            .not_null()
                            .primary_key(),
                    )
                    .col(ColumnDef::new(Trade::UserId).big_integer().not_null())
                    .col(ColumnDef::new(Trade::Status).tiny_integer().not_null())
                    .col(ColumnDef::new(Trade::Level).tiny_integer().not_null())
                    .col(ColumnDef::new(Trade::Period).tiny_integer().not_null())
                    .col(ColumnDef::new(Trade::Total).integer().not_null())
                    .col(ColumnDef::new(Trade::CreateTime).timestamp().default(Expr::current_timestamp()).not_null())
                    .col(ColumnDef::new(Trade::ValidTime).timestamp().not_null())
                    .col(ColumnDef::new(Trade::StartTime).timestamp().not_null())
                    .col(ColumnDef::new(Trade::PayTime).timestamp().null())
                    .to_owned(),
            )
            .await
    }

    async fn down(&self, manager: &SchemaManager) -> Result<(), DbErr> {
        manager
            .drop_table(Table::drop().table(Trade::Table).to_owned())
            .await
    }
}

#[derive(DeriveIden)]
enum Trade {
    Table,
    Id,
    UserId,
    Status,
    Level,
    Period,
    Total,
    CreateTime,
    ValidTime,
    PayTime,
    StartTime,
}
