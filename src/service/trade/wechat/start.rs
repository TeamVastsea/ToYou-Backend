use std::time::Duration;

use chrono::{Local, NaiveDate, SecondsFormat, TimeDelta, Utc};
use lazy_static::lazy_static;
use moka::future::Cache;
use sea_orm::{ActiveModelTrait, NotSet};
use sea_orm::ActiveValue::Set;
use wechat_pay_rust_sdk::model::NativeParams;

use crate::DATABASE;
use crate::service::trade::{TradeStatus, WECHAT_PAY_CLIENT};
use crate::service::user::level::Level;

lazy_static! {
    static ref TRADE_CACHE: Cache<i64, String> = Cache::builder()
        .time_to_live(Duration::from_secs(60 * 10)) //trade expires in 10 minutes
        .build();
}

pub fn generate_trade_id(prefix: &str) -> String {
    let mut id = prefix.to_string().to_ascii_uppercase();

    id += Utc::now().format("%Y%m%d%H%M%S%b%a%f").to_string().to_ascii_uppercase().as_str();
    id = (&id[..32]).to_string();

    id
}

pub async fn start_wechat(userid: i64, level: Level, period: i32, start_date: NaiveDate) -> (bool, String) {
    if let Some(_) = TRADE_CACHE.get(&userid).await {
        return (false, "Already exist".to_string());
    }
    let trade_id = generate_trade_id("WECHAT");
    TRADE_CACHE.insert(userid, trade_id.clone()).await;

    let date = Local::now().checked_add_signed(TimeDelta::minutes(10)).unwrap();
    let trade_id = generate_trade_id("WECHAT").to_string();
    let years = period / 12;
    let amount = if userid == 1 && userid == 2 {
        1
    } else {
        level.get_price() * (period - years * 2)
    };

    let mut pay_params = NativeParams::new(
        &format!("图邮 {}-{}月", level, period),
        &trade_id,
        amount.into(),
    );
    pay_params.time_expire = Some(date.to_rfc3339_opts(SecondsFormat::Secs, false));
    let body = WECHAT_PAY_CLIENT.native_pay(pay_params).await.unwrap();

    let trade = crate::model::trade::ActiveModel {
        id: Set(trade_id),
        user_id: Set(userid),
        status: Set(TradeStatus::NotPay as i16),
        level: Set(level as i16),
        period: Set(period as i16),
        total: Set(amount),
        create_time: NotSet,
        valid_time: Set(date.naive_local()),
        start_time: Set(start_date),
        pay_time: NotSet,
    };
    trade.insert(&*DATABASE).await.unwrap();

    if body.code_url.is_none() {
        return (false, format!("Cannot get url: {:?}, code {:?}", body.message, body.code));
    }

    return (true, body.code_url.unwrap());
}

#[test]
fn test_generate_trade_id() {
    let id = generate_trade_id("TESTID");
    assert!(id.starts_with("TESTID"));
    assert_eq!(id.len(), 32);

    let url = tokio::runtime::Runtime::new().unwrap().block_on(start_wechat(1, Level::Started, 12, Utc::now()));
    println!("trade_id: {id}");
    println!("pay_url: {url:?}");
    let cert = tokio::runtime::Runtime::new().unwrap().block_on(WECHAT_PAY_CLIENT.certificates());
    println!("{:?}", cert);
}