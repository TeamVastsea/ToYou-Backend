package cc.vastsea.toyou.task;

import cc.vastsea.toyou.common.StatusCode;
import cc.vastsea.toyou.exception.BusinessException;
import cc.vastsea.toyou.model.entity.Picture;
import cc.vastsea.toyou.service.PictureService;
import com.github.benmanes.caffeine.cache.Cache;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import javax.print.PrintService;
import java.util.Date;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

import static cc.vastsea.toyou.service.impl.PictureServiceImpl.waitSavePictureCache;

@Component
@Slf4j
public class CleanupEndTask implements Runnable{

    @Autowired
    private TaskScheduler taskScheduler;

    @Autowired
    private PictureService pictureService;

    public static final ReentrantLock cleanupLock = new ReentrantLock();

    public void scheduleTask(){
        Date startTime = new Date(System.currentTimeMillis() + 10*60*1000);
        taskScheduler.schedule(this, startTime);
    }

    public boolean save(){
        try {
            cleanupLock.lock();
            pictureService.setAllowSaveToDatabase(true);
            ConcurrentMap<String, Picture> stringPictureConcurrentMap = pictureService.getWaitSavePictureCache().asMap();
            if(!stringPictureConcurrentMap.isEmpty()){
                log.info("写入清理工作开始时堆积缓存");
                stringPictureConcurrentMap.values().forEach(picture -> {
                    boolean save = pictureService.save(picture);
                    if(!save){
                        throw new BusinessException(StatusCode.INTERNAL_SERVER_ERROR, "添加失败，数据库错误(写入清理过程中，用户上传产生缓存时)");
                    }
                });
                log.info("清理过程中用户上传了"+stringPictureConcurrentMap.size()+"张图片，已写入数据库");
                return false;
            }
            pictureService.getWaitSavePictureCache().cleanUp();
            return true;
        }finally {
            cleanupLock.unlock();
        }
    }

    @Override
    public void run() {
        if(!this.save()){
            throw new BusinessException(StatusCode.INTERNAL_SERVER_ERROR, "清理任务未发送结束请求.已强制写入缓存");
        }
    }
}
