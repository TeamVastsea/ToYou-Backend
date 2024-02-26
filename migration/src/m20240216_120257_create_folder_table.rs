use sea_orm_migration::prelude::*;

#[derive(DeriveMigrationName)]
pub struct Migration;

#[async_trait::async_trait]
impl MigrationTrait for Migration {
    async fn up(&self, manager: &SchemaManager) -> Result<(), DbErr> {
        manager
            .create_table(
                Table::create()
                    .table(Folder::Table)
                    .if_not_exists()
                    .col(ColumnDef::new(Folder::Id).big_integer().not_null().auto_increment().primary_key())
                    .col(ColumnDef::new(Folder::Name).string().not_null())
                    .col(ColumnDef::new(Folder::Parent).big_integer().null())
                    .col(ColumnDef::new(Folder::Child).array(ColumnType::BigInteger).null())
                    .col(ColumnDef::new(Folder::UserId).big_integer().not_null())
                    .col(ColumnDef::new(Folder::Size).double().not_null())
                    .col(ColumnDef::new(Folder::Depth).small_integer().default(0).not_null())
                    .col(ColumnDef::new(Folder::CreateTime).timestamp().default(Expr::current_timestamp()).not_null())
                    .col(ColumnDef::new(Folder::UpdateTime).timestamp().default(Expr::current_timestamp()).not_null())
                    .to_owned(),
            )
            .await
    }

    async fn down(&self, manager: &SchemaManager) -> Result<(), DbErr> {
        manager
            .drop_table(Table::drop().table(Folder::Table).to_owned())
            .await
    }
}

#[derive(DeriveIden)]
enum Folder {
    Table,
    Id,
    Name,
    Parent,
    Child,
    UserId,
    Size,
    Depth,
    CreateTime,
    UpdateTime,
}
