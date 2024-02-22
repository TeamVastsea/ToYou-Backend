use std::cmp::max;
use std::io::Error;

use image::{ColorType, DynamicImage, GenericImageView};
use image::codecs::jpeg::JpegEncoder;
use image::imageops::overlay;

use crate::service::picture::file::read_image;

pub struct ImageFile {
    file: DynamicImage,
}

impl ImageFile {
    pub async fn new(file_id: &str) -> Result<Self, Error> {
        let file = read_image(file_id).await;

        match file {
            Some(file) => {
                Ok(Self { file })
            }
            None => Err(Error::new(std::io::ErrorKind::NotFound, "File not found or format not recognized."))
        }
    }
    
    pub async fn compress(&mut self) {
        let (width, height) = self.file.dimensions();
        let compressed = if width > height {
            self.file.thumbnail(2000, 1000)
        } else {
            self.file.thumbnail(1000, 2000)
        };
        self.file = compressed;
    }
    
    pub async fn watermark(&mut self, logo: &DynamicImage) {
        let (width, height) = self.file.dimensions();
        let max_height = (width as f64 * 0.7 / 4.6).min(height as f64 * 0.7);
        let fit_height = (width as f64 * 0.2 / 4.6).max(height as f64 * 0.2).min(max_height) as u32;
        
        let logo_fit = logo.thumbnail(fit_height * 4.6 as u32, fit_height);
        let x = max(0, width as i64 - logo_fit.width() as i64 - (fit_height as f32 * 4.6 * 0.02) as i64);
        let y = max(0, height as i64 - logo_fit.height() as i64 - (fit_height as f32 * 0.1) as i64);
        overlay(&mut self.file, &logo_fit, x, y);
    }
    
    pub fn encode(&self) -> Result<Vec<u8>, Error> {
        let mut buffer = Vec::new();
        JpegEncoder::new(&mut buffer).encode(self.file.to_rgba8().as_raw(), self.file.width(), self.file.height(), self.file.color()).unwrap();
        Ok(buffer)
    }
    
    pub fn encode_preview(&self) -> Result<Vec<u8>, Error> {
        let mut buffer = Vec::new();
        JpegEncoder::new_with_quality(&mut buffer, 80).encode(self.file.to_rgba8().as_raw(), self.file.width(), self.file.height(), ColorType::Rgba8).unwrap();
        Ok(buffer)
    }
}