package com.example.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.entity.dto.Account;
import com.example.entity.dto.StoreImage;
import com.example.mapper.AccountMapper;
import com.example.mapper.ImageStoreMapper;
import com.example.service.ImageService;
import com.example.utils.Const;
import com.example.utils.FlowUtils;
import io.minio.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.IOUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
public class ImageServiceImpl extends ServiceImpl<ImageStoreMapper, StoreImage> implements ImageService {

    @Resource
    MinioClient client;

    @Resource
    AccountMapper mapper;

    @Resource
    FlowUtils flowUtils;

    SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");

    @Override
    public void fetchImageFromMinio(OutputStream stream, String imagePath) throws Exception {
        GetObjectArgs args = GetObjectArgs.builder()
                .bucket("test")
                .object(imagePath)
                .build();
        GetObjectResponse object = client.getObject(args);
        IOUtils.copy(object, stream);
    }

    @Override
    public String uploadImage(int id, MultipartFile file) throws IOException {
        String key = Const.FORUM_IMAGE_COUNTER + id;
        if(!flowUtils.limitPeriodCounterCheck(key, 20, 3600)) return null;
        String imageName = UUID.randomUUID().toString().replace("-", "");
        Date date = new Date();
        imageName = "/cache/" + format.format(date) + "/" + imageName;
        PutObjectArgs args = PutObjectArgs.builder()
                .bucket("test")
                .stream(file.getInputStream(), file.getSize(), -1)
                .object(imageName)
                .build();
        try {
            client.putObject(args);
            if(this.save(new StoreImage(id, imageName, date))){
                return imageName;
            } else {
                return null;
            }
        }catch (Exception e) {
            log.error("图片上传出现问题: "+e.getMessage(), e);
            return null;
        }
    }

    @Override
    public String uploadAvatar(int id, MultipartFile file) throws IOException {
        //使用随机UUID作为图片名字，而不是直接用户ID是因为用户更改头像之后可以立即更新缓存
        String imageName = UUID.randomUUID().toString().replace("-", "");
        imageName = "/avatar/" + imageName;
        PutObjectArgs args = PutObjectArgs.builder()
                .bucket("test")
                .stream(file.getInputStream(), file.getSize(), -1)
                .object(imageName)
                .build();
        try {
            String avatar = mapper.selectById(id).getAvatar();
            client.putObject(args);
            if(mapper.update(null, Wrappers.<Account>update()
                    .eq("id", id)
                    .set("avatar", imageName)) > 0) {
                this.deleteOldAvatar(avatar);
                return imageName;
            } else {
                return null;
            }
        } catch (Exception e) {
            log.error("图片上传出现问题: "+e.getMessage(), e);
            return null;
        }
    }

    private void deleteOldAvatar(String avatar) throws Exception {
        if(avatar == null || avatar.isEmpty()) return;
        RemoveObjectArgs remove = RemoveObjectArgs.builder()
                .bucket("test")
                .object(avatar)
                .build();
        client.removeObject(remove);
    }
}
