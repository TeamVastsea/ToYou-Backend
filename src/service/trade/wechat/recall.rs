use axum::http::HeaderMap;
use axum::Json;
use base64::Engine;
use sea_orm::{ActiveModelTrait, EntityTrait, IntoActiveModel};
use sea_orm::ActiveValue::Set;
use serde::Serialize;
use tracing::{debug, error, info};
use wechat_pay_rust_sdk::model::WechatPayNotify;
use wechat_pay_rust_sdk::pay::PayNotifyTrait;
use wechat_pay_rust_sdk::response::Certificate;
use x509_parser::parse_x509_certificate;
use x509_parser::pem::parse_x509_pem;

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
    
    let pub_key = get_public_key().await;
    let timestamp = header_map.get("Wechatpay-Timestamp").unwrap().to_str().unwrap();
    let nonce = header_map.get("Wechatpay-Nonce").unwrap().to_str().unwrap();
    let signature = header_map.get("Wechatpay-Signature").unwrap().to_str().unwrap();
    let body = body.as_str();
    debug!("Wechat pay recall: {pub_key:?} {timestamp:?} {nonce:?} {signature:?} {body:?}");

    //verify signature
    if WECHAT_PAY_CLIENT.verify_signatrue(
        &pub_key,
        &timestamp,
        &nonce,
        &signature,
        &body,
    ).map_err(|e| {error!("{}", e.to_string())}).is_err() {
        return Json(WechatNoticeResponse {
            code: "FAIL".to_string(),
            message: "签名错误".to_string(),
        });
    }

    let mut trade = Trade::find_by_id(id).one(&*crate::DATABASE).await.unwrap().unwrap().into_active_model();
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

#[test]
fn test_cert() {
    let pem = tokio::runtime::Runtime::new().unwrap().block_on(get_public_key());
    println!("{pem}");
}

async fn get_public_key() -> String {
    //get cert from WeChat
    let cert = WECHAT_PAY_CLIENT.certificates().await.unwrap();
    let data: Certificate = cert.data.unwrap()[0].clone();
    let ciphertext = data.encrypt_certificate.ciphertext;
    let nonce = data.encrypt_certificate.nonce;
    let associated_data = data.encrypt_certificate.associated_data;
    let cert = WECHAT_PAY_CLIENT.decrypt_bytes(ciphertext, nonce, associated_data).unwrap();
    
    //extract public key
    let res = parse_x509_pem(&cert).unwrap();
    let res_x509 = parse_x509_certificate(&res.1.contents).unwrap().1;
    let public_key = res_x509.public_key().raw;
    let pem = base64::prelude::BASE64_STANDARD.encode(public_key);
    format!("-----BEGIN PUBLIC KEY-----\n{}\n-----END PUBLIC KEY-----", pem)
}