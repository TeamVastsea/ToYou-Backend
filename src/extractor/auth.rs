use axum::async_trait;
use axum::extract::FromRequestParts;
use axum::http::request::Parts;

use crate::service::error::ErrorMessage;
use crate::service::user::login::login_by_token;

pub struct AuthUser(pub crate::model::user::Model);

#[async_trait]
impl<S> FromRequestParts<S> for AuthUser
    where S: Send + Sync {
    type Rejection = ErrorMessage;

    async fn from_request_parts(parts: &mut Parts, _state: &S) -> Result<Self, Self::Rejection> {
        let headers = &parts.headers;
        let user = login_by_token(headers).await
            .ok_or(ErrorMessage::InvalidToken)?;

        Ok(AuthUser(user))
    }
}