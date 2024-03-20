use axum::http::HeaderMap;
use axum::Json;
use sea_orm::{ActiveModelTrait, EntityTrait, IntoActiveModel};
use sea_orm::ActiveValue::Set;
use serde::Serialize;
use tracing::{error, info};
use wechat_pay_rust_sdk::model::WechatPayNotify;
use wechat_pay_rust_sdk::pay::PayNotifyTrait;
use crate::model::prelude::Trade;
use crate::service::trade::{TradeStatus, WECHAT_PAY_CLIENT, WECHAT_PUBLIC};

pub async fn wechat_pay_recall(header_map: HeaderMap, body: String) -> Json<WechatNoticeResponse> {
    let request: WechatPayNotify = serde_json::from_str(&body).unwrap();
    
    //verify trade status
    let nonce = request.resource.nonce;
    let chiphertext = request.resource.ciphertext;
    let associated_data = request.resource.associated_data.unwrap_or_default();
    let result = WECHAT_PAY_CLIENT.decrypt_paydata(&chiphertext, &nonce, &associated_data).unwrap();
    let id = result.out_trade_no.clone();
    info!("Wechat pay recall: {result:?}");
    let mut trade = Trade::find_by_id(id).one(&*crate::DATABASE).await.unwrap().unwrap().into_active_model();

    let cert = WECHAT_PAY_CLIENT.certificates().await.unwrap();
    //verify signature
    if WECHAT_PAY_CLIENT.verify_signatrue(
        &WECHAT_PUBLIC,
        header_map.get("Wechatpay-Timestamp").unwrap().to_str().unwrap(),
        header_map.get("Wechatpay-Nonce").unwrap().to_str().unwrap(),
        header_map.get("Wechatpay-Signature").unwrap().to_str().unwrap(),
        serde_json::to_string(&body).unwrap().as_str(),
    ).map_err(|e| {error!("{}", e.to_string())}).is_err() {
        return Json(WechatNoticeResponse {
            code: "FAIL".to_string(),
            message: "签名错误".to_string(),
        });
    }
    
    trade.status = Set(TradeStatus::Paid as i16);
    trade.pay_time = Set(Some(chrono::Local::now().naive_local()));
    trade.save(&*crate::DATABASE).await.unwrap();

    Json(WechatNoticeResponse {
        code: "SUCCESS".to_string(),
        message: "成功".to_string(),
    })
}

#[derive(Serialize, Clone)]
pub struct WechatNoticeResponse {
    code: String,
    message: String,
}