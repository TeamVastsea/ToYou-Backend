use sea_orm_migration::prelude::*;

#[derive(DeriveMigrationName)]
pub struct Migration;

#[async_trait::async_trait]
impl MigrationTrait for Migration {
    async fn up(&self, manager: &SchemaManager) -> Result<(), DbErr> {
        manager
            .create_table(
                Table::create()
                    .table(User::Table)
                    .if_not_exists()
                    .col(
                        ColumnDef::new(User::Id)
                            .integer()
                            .not_null()
                            .auto_increment()
                            .primary_key(),
                    )
                    .col(
                        ColumnDef::new(User::Username)
                            .string()
                            .unique_key()
                            .not_null(),
                    )
                    .col(ColumnDef::new(User::Password).string_len(65_u32).not_null())
                    // TJDWgyZMeJsBsoLt6Rd4$IEt9C7+nzB8sqJcQJLZblsQF/50stiRJDcF6pby1zjo=
                    .col(ColumnDef::new(User::Phone).string().unique_key().not_null())
                    .col(ColumnDef::new(User::Email).string().null())
                    .col(
                        ColumnDef::new(User::Available)
                            .boolean()
                            .default(true)
                            .not_null(),
                    )
                    .col(
                        ColumnDef::new(User::Level)
                            .json()
                            .default("[]")
                            .not_null(),
                    )
                    .col(
                        ColumnDef::new(User::CreateTime)
                            .timestamp()
                            .default(Expr::current_timestamp())
                            .not_null(),
                    )
                    .col(
                        ColumnDef::new(User::UpdateTime)
                            .timestamp()
                            .default(Expr::current_timestamp())
                            .not_null(),
                    )
                    .to_owned(),
            )
            .await
    }

    async fn down(&self, manager: &SchemaManager) -> Result<(), DbErr> {
        manager
            .drop_table(Table::drop().table(User::Table).to_owned())
            .await
    }
}

#[derive(DeriveIden)]
enum User {
    Table,
    Id,
    Username,
    Password,
    Phone,
    Email,
    Available,
    Level,
    CreateTime,
    UpdateTime,
}
