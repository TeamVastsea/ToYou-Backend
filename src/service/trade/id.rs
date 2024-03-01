use std::time::Duration;

use chrono::Utc;
use lazy_static::lazy_static;
use moka::future::Cache;

lazy_static!{
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

#[test]
fn test_generate_trade_id() {
    let id = generate_trade_id("TESTID");
    assert!(id.starts_with("TESTID"));
    assert_eq!(id.len(), 32);
    println!("trade_id: {id}");
}