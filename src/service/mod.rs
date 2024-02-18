use std::time::Duration;
use lazy_static::lazy_static;

use moka::future::Cache;

pub mod picture;
pub mod user;

lazy_static!{
    static ref TOKEN_CACHE: Cache<String, i64> = Cache::builder()
        .time_to_idle(Duration::from_secs(60 * 60 * 24 * 7)) //if the key is not accessed for 7 days, it will be removed
        .build();
}
