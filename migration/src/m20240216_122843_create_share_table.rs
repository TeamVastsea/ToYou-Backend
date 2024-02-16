use sea_orm_migration::prelude::*;

#[derive(DeriveMigrationName)]
pub struct Migration;

#[async_trait::async_trait]
impl MigrationTrait for Migration {
    async fn up(&self, manager: &SchemaManager) -> Result<(), DbErr> {
        manager
            .create_table(
                Table::create()
                    .table(Share::Table)
                    .if_not_exists()
                    .col(
                        ColumnDef::new(Share::Id)
                            .uuid()
                            .not_null()
                            .extra("DEFAULT gen_random_uuid()")
                            .primary_key(),
                    )
                    .col(ColumnDef::new(Share::Content).array(ColumnType::String(None)).not_null())
                    .col(ColumnDef::new(Share::Password).string().null())
                    .col(ColumnDef::new(Share::UserId).integer().not_null())
                    .col(ColumnDef::new(Share::CreateTime).timestamp().default(Expr::current_timestamp()).not_null())
                    .col(ColumnDef::new(Share::ValidTime).timestamp().not_null())
                    .to_owned(),
            )
            .await
    }

    async fn down(&self, manager: &SchemaManager) -> Result<(), DbErr> {
        manager
            .drop_table(Table::drop().table(Share::Table).to_owned())
            .await
    }
}

#[derive(DeriveIden)]
enum Share {
    Table,
    Id,
    Content,
    Password,
    UserId,
    CreateTime,
    ValidTime,
}
