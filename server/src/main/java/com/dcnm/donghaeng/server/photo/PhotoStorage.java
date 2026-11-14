package com.dcnm.donghaeng.server.photo;

/** 사진 파일 보관. 개발(LocalPhotoStorage)과 배포(S3PhotoStorage)가 같은 모양을 사용. */
public interface PhotoStorage {

    void put(String key, byte[] bytes, String contentType);

    byte[] get(String key);

    void delete(String key);
}