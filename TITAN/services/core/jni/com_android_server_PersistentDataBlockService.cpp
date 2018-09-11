/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <android_runtime/AndroidRuntime.h>
#include <JNIHelp.h>
#include <jni.h>

#include <utils/misc.h>
#include <sys/ioctl.h>
#include <sys/mount.h>
#include <utils/Log.h>


#include <inttypes.h>
#include <fcntl.h>
#include <errno.h>
#include <string.h>

#define PRODUCT_PATH "/dev/block/bootdevice/by-name/product"
#define ROOTM_PATH "/dev/block/bootdevice/by-name/rootm"
int proinfo_fd = 0;

// the rootm.bin info format.
// 4+4+(512-8),so read count value only.
struct rootm_info_format{
       int magic;
       int count;
       char reserved[512-8];
};

namespace android {

    uint64_t get_block_device_size(int fd)
    {
        uint64_t size = 0;
        int ret;

        ret = ioctl(fd, BLKGETSIZE64, &size);

        if (ret)
            return 0;

        return size;
    }

    int wipe_block_device(int fd)
    {
        uint64_t range[2];
        int ret;
        uint64_t len = get_block_device_size(fd);

        range[0] = 0;
        range[1] = len;

        if (range[1] == 0)
            return 0;

        ret = ioctl(fd, BLKSECDISCARD, &range);
        if (ret < 0) {
            ALOGE("Something went wrong secure discarding block: %s\n", strerror(errno));
            range[0] = 0;
            range[1] = len;
            ret = ioctl(fd, BLKDISCARD, &range);
            if (ret < 0) {
                ALOGE("Discard failed: %s\n", strerror(errno));
                return -1;
            } else {
                ALOGE("Wipe via secure discard failed, used non-secure discard instead\n");
                return 0;
            }

        }

        return ret;
    }

    static jlong com_android_server_PersistentDataBlockService_getBlockDeviceSize(JNIEnv *env, jclass, jstring jpath)
    {
        const char *path = env->GetStringUTFChars(jpath, 0);

        int fd = open(path, O_RDONLY);

        if (fd < 0)
            return 0;

        return get_block_device_size(fd);
    }

    static int com_android_server_PersistentDataBlockService_wipe(JNIEnv *env, jclass, jstring jpath) {
        const char *path = env->GetStringUTFChars(jpath, 0);
        int fd = open(path, O_WRONLY);

        if (fd < 0)
            return 0;

        return wipe_block_device(fd);
    }

    static int com_android_server_OpenPartition(JNIEnv *env, jclass) {
	uint64_t size = 0;
        
	int ret;

	proinfo_fd = open(PRODUCT_PATH, O_RDWR);
        
	if (proinfo_fd < 0)
        {
            ALOGE("leo open product.mbn fail:%d\n",proinfo_fd);
            return 0;
        }
	 else
	 {
	      ALOGE("leo open product.mbn success\n");	
	      ret=ioctl(proinfo_fd, BLKGETSIZE64, &size);
	      if(ret)	
		    ALOGE("leo get product.mbn size fail\n"); 	
	      else
		    ALOGE("leo get product.mbn size success:%lld\n",size);
	 }

        return proinfo_fd;
    }	

    static int com_android_server_ReadPartition(JNIEnv *env, jclass,int offset, int index, char readbuf[]) {
	
	int read_size;
        if(proinfo_fd > 0)
        {
	     /*now read product.mbn the secend section info , size=50 step by step*/
	     ALOGE("leo open product.mbn successed!\n");
	     /*the proinfo_fd = 0,by default*/

       	     /*move proinfo_fd by seek*/
             if(lseek(proinfo_fd,512+index*offset,SEEK_SET) == -1)
	     {
			printf("lseek error!\n");
			return -1;
	     } 
	     /*now the proinfo_fd = 512, the second section*/ 
             read_size = read(proinfo_fd,readbuf,50);
          
	     if(read_size == 50)
	     {
    		    if(!strncmp(readbuf+40,"0123456789",10))
                    {
                       ALOGE("leo read info success!\n");
                       memset(readbuf+40,0x0,10); 
		       return read_size;
                    }
                    else
                    {
                       memset(readbuf,0x0,50);
                       ALOGE("1leo read info failed!\n"); 
                    }
	     }
	     else
	     {
                    memset(readbuf,0x0,50);
		    ALOGE("2leo read info failed!\n"); 		

	     }
        }
	 else
	 {
                memset(readbuf,0x0,50);
	 	ALOGE("3leo read product.mbn fail\n"); 	
	 } 

	return 0;
    }		

    //Write product.mbn the second section informations
    //The info array same as Read function
    // offset=50 
    // index=0,1,2,3 reference SN,MEID,IMEI,MACWLAN
    static int com_android_server_WritePartition(JNIEnv *env, jclass, int offset,int index, char writebuf[]) {
	
	int write_size;
        unsigned char write_str[51];
	

	if(proinfo_fd > 0)
	{
	   /*now write product.mbn the second section , size=50 step by step*/
           ALOGE("leo open product.mbn successed!\n");
           /*the proinfo_fd = 0,by default*/
		
 	   /*move proinfo_fd by seek*/
           if(lseek(proinfo_fd,512+index*offset,SEEK_SET) == -1)
	   {
			printf("lseek error!\n");
			return -1;
	   }
           memset(write_str,0x0,51);
           memcpy(write_str,writebuf,strlen(writebuf));
           memcpy(write_str+40,"0123456789",10);
	   /*now the proinfo_fd = 512, the second section*/ 
	   write_size = write(proinfo_fd,write_str,50);

	   if(sync() != 0)
	   {
		ALOGE("sync error!\n");
	   }

           if(write_size == 50)
	   {
		   ALOGE("leo write info success:%d\n",write_size);
	   }
	   else
	   {
		   ALOGE("leo write info failed!\n");
	   }
           return write_size;
	}
	else
	{
		ALOGE("leo failed open product.mbn!\n");
	}

	return 0;
    }


    static void com_android_server_ClosePartition(JNIEnv *env, jclass) {

        if (proinfo_fd > 0)
        {
            close(proinfo_fd);
	     ALOGE("Close partition success\n");
        }
	 else
	     ALOGE("Close partition fail\n");	
    }		

   static int com_android_server_CheckRootOrNot(JNIEnv *env, jclass) {

       uint64_t size;
       int ret;
       int rootm_fd;
       int read_size;
       struct rootm_info_format rootm_info;
       int return_value=0;
       int count = 0;

       //open
       rootm_fd = open(ROOTM_PATH, O_RDONLY);

       if(rootm_fd <= 0)
       {
         ALOGE("leo open rootm.bin fail!");
         return 2;
       }

       //read the 512 size
       read_size = read(rootm_fd,&rootm_info,512);

       if(read_size == 512)
       {
           ALOGE("leo read rootm.bin success!\n");
           count = rootm_info.count;

           if(rootm_info.count <=0)
           {
                  ALOGE("leo show nerver be root ,the count=%x\n",count);
                  return_value = 0;;
           }
           else
           {
                  ALOGE("leo show already be root, the count=%x\n",count);
                  return_value = 1;
           }

      }
      else
      {
           ALOGE("leo read rootm.bin failed!\n");
           return_value=2;
      }


      //close
      close(rootm_fd);
      return return_value;
   }

    static JNINativeMethod sMethods[] = {
         /* name, signature, funcPtr */
        {"nativeGetBlockDeviceSize", "(Ljava/lang/String;)J", (void*)com_android_server_PersistentDataBlockService_getBlockDeviceSize},
        {"nativeWipe", "(Ljava/lang/String;)I", (void*)com_android_server_PersistentDataBlockService_wipe},
        {"nativeopenpartition", "()I", (void*)com_android_server_OpenPartition},
        {"nativeclosepartition", "()V", (void*)com_android_server_ClosePartition},
        {"nativereadpartition", "(II[C)I", (void*)com_android_server_ReadPartition},
        {"nativewritepartition", "(II[C)I", (void*)com_android_server_WritePartition},
	{"nativecheckrootornot", "()I",(void*)com_android_server_CheckRootOrNot},
    };

    int register_android_server_PersistentDataBlockService(JNIEnv* env)
    {
        return jniRegisterNativeMethods(env, "com/android/server/PersistentDataBlockService",
                                        sMethods, NELEM(sMethods));
    }

} /* namespace android */
