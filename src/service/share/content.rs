#[derive(Clone, Debug, PartialEq, Eq)]
pub enum ContentType{
    Folder(i64),
    Picture(i64)
}

impl ContentType {
    pub fn verify(data: &str) -> bool {
        let id = Self::try_from(data);
        return id.is_ok();
    }
}

impl TryFrom<&str> for ContentType {
    type Error = std::io::Error;

    fn try_from(value: &str) -> Result<Self, Self::Error> {
        let id = &value[1..].parse();
        if id.is_err() {
            Err(std::io::Error::new(std::io::ErrorKind::InvalidInput, "Invalid input"))
        } else {
            let id = id.clone().unwrap();
            match value.chars().next().unwrap() {
                'f' => Ok(ContentType::Folder(id)),
                'p' => Ok(ContentType::Picture(id)),
                _ => Err(std::io::Error::new(std::io::ErrorKind::InvalidInput, "Invalid input"))
            }
        }
    }
}

#[test]
fn test_parse_content_type() {
    let folder = ContentType::try_from("f1");
    assert_eq!(folder.unwrap(), ContentType::Folder(1));
    let picture = ContentType::try_from("p1");
    assert_eq!(picture.unwrap(), ContentType::Picture(1));
    let invalid = ContentType::try_from("x.1");
    assert!(invalid.is_err());
    println!("Share Content Parse Test passed");
}