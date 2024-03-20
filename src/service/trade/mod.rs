use std::fs;

use lazy_static::lazy_static;
use wechat_pay_rust_sdk::pay::WechatPay;

use crate::CONFIG;

pub mod wechat;

lazy_static! {
    static ref WECHAT_PRIVATE: String = fs::read_to_string(&CONFIG.wechat.private_key).unwrap();
    static ref WECHAT_PUBLIC: String = fs::read_to_string(&CONFIG.wechat.public_key).unwrap();
    static ref WECHAT_PAY_CLIENT: WechatPay = WechatPay::new(
        &CONFIG.wechat.app_id,
        &CONFIG.wechat.mch_id,
        &WECHAT_PRIVATE,
        &CONFIG.wechat.serial,
        &CONFIG.wechat.key,
        &CONFIG.wechat.call_back_url,
    );
}

enum TradeStatus {
    NotPay = 0,
    Paid,
    Refund,
    Error,
}