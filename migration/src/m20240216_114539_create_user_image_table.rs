use sea_orm_migration::prelude::*;

#[derive(DeriveMigrationName)]
pub struct Migration;

#[async_trait::async_trait]
impl MigrationTrait for Migration {
    async fn up(&self, manager: &SchemaManager) -> Result<(), DbErr> {
        manager
            .create_table(
                Table::create()
                    .table(UserImage::Table)
                    .if_not_exists()
                    .col(
                        ColumnDef::new(UserImage::Id)
                            .integer()
                            .not_null()
                            .auto_increment()
                            .primary_key(),
                    )
                    .col(ColumnDef::new(UserImage::ImageId).string().not_null())
                    .col(ColumnDef::new(UserImage::UserId).integer().not_null())
                    .col(ColumnDef::new(UserImage::FileName).string().not_null())
                    .col(ColumnDef::new(UserImage::FolderId).integer().not_null())
                    .col(ColumnDef::new(UserImage::CreateTime).timestamp().default(Expr::current_timestamp()).not_null())
                    .col(ColumnDef::new(UserImage::UpdateTime).timestamp().default(Expr::current_timestamp()).not_null())
                    .to_owned(),
            )
            .await
    }

    async fn down(&self, manager: &SchemaManager) -> Result<(), DbErr> {
        manager
            .drop_table(Table::drop().table(UserImage::Table).to_owned())
            .await
    }
}

#[derive(DeriveIden)]
enum UserImage {
    Table,
    Id,
    UserId,
    ImageId,
    FileName,
    FolderId,
    CreateTime,
    UpdateTime
}
