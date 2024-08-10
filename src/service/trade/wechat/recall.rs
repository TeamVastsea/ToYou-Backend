use axum::http::HeaderMap;
use base64::Engine;
use rsa::{Pkcs1v15Sign, RsaPublicKey};
use rsa::pkcs8::DecodePublicKey;
use rsa::signature::digest::Digest;
use sea_orm::{ActiveModelTrait, EntityTrait, IntoActiveModel};
use sea_orm::ActiveValue::Set;
use serde::Serialize;
use sha2::Sha256;
use tracing::error;
use wechat_pay_rust_sdk::model::WechatPayNotify;
use wechat_pay_rust_sdk::pay::PayNotifyTrait;
use wechat_pay_rust_sdk::response::Certificate;
use x509_parser::parse_x509_certificate;
use x509_parser::pem::parse_x509_pem;

use crate::model::prelude::Trade;
use crate::service::trade::{TradeStatus, WECHAT_PAY_CLIENT};
use crate::service::user::level::{add_level_to_user, Level};

pub async fn wechat_pay_recall(header_map: HeaderMap, body: String) -> Result<String, String> {
    let request: WechatPayNotify = serde_json::from_str(&body).unwrap();

    //verify trade status
    let nonce = request.resource.nonce;
    let chiphertext = request.resource.ciphertext;
    let associated_data = request.resource.associated_data.unwrap_or_default();
    let result = WECHAT_PAY_CLIENT.decrypt_paydata(&chiphertext, &nonce, &associated_data).unwrap();
    let id = result.out_trade_no.clone();

    let trade = Trade::find_by_id(id).one(&*crate::DATABASE).await.unwrap().ok_or(r#"{{"code": "FAIL", "message": "Trade not found"}}"#.to_string())?;
    if trade.status == TradeStatus::Paid as i16 || trade.status == TradeStatus::Refund as i16 {
        return Ok("".to_string());
    }

    let pub_key = get_public_key().await;
    let timestamp = header_map.get("Wechatpay-Timestamp").unwrap().to_str().unwrap();
    let nonce = header_map.get("Wechatpay-Nonce").unwrap().to_str().unwrap();
    let signature = header_map.get("Wechatpay-Signature").unwrap().to_str().unwrap();
    let body = body.as_str();

    //verify signature
    verify_signature(&pub_key, timestamp, nonce, signature, body)
        .map_err(|e| format!(r#"{{"code": "FAIL", "message": "{}"}}"#, e))?;

    let res = add_level_to_user(trade.user_id, Level::from(trade.level as u8), trade.period, trade.start_time).await;
    if res.is_err() {
        let mut trade = trade.into_active_model();
        trade.status = Set(TradeStatus::Error as i16);
        trade.save(&*crate::DATABASE).await.unwrap();
        error!("Add level failed: {}", res.err().unwrap());
        return Ok(r#"{{"code": "FAIL", "message": "Add level failed"}}"#.to_string());
    }
    let mut trade = trade.into_active_model();
    trade.status = Set(TradeStatus::Paid as i16);
    trade.pay_time = Set(Some(chrono::Local::now().naive_local()));
    trade.save(&*crate::DATABASE).await.unwrap();


    Ok("".to_string())
}

#[derive(Serialize, Clone)]
pub struct WechatNoticeResponse {
    code: String,
    message: String,
}

#[test]
fn test_cert() {
    let pem = tokio::runtime::Runtime::new().unwrap().block_on(get_public_key());
    println!("{pem:?}");
}

async fn get_public_key() -> Vec<u8> {
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

    public_key.to_vec()
}

fn verify_signature(pub_key: &[u8], timestamp: &str, nonce: &str, signature: &str, body: &str) -> Result<(), String> {
    let message = format!(
        "{}\n{}\n{}\n",
        timestamp,
        nonce,
        body
    );
    let pub_key = RsaPublicKey::from_public_key_der(pub_key)
        .map_err(|e| e.to_string())?;
    let hashed = Sha256::new().chain_update(message).finalize();
    let signature = base64::prelude::BASE64_STANDARD.decode(signature.as_bytes())
        .map_err(|e| e.to_string())?;
    let scheme = Pkcs1v15Sign::new::<Sha256>();
    pub_key
        .verify(scheme, &hashed, signature.as_slice())
        .map_err(|e| e.to_string())
}