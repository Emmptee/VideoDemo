#include "yuvconvert.h"
#include <stdio.h>
#include<android/log.h>

#define TAG "JNI" // 这个是自定义的LOG的标识
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,TAG ,__VA_ARGS__) // 定义LOGE类型

#define MR(a,b,c) ( (1.164*(a-16)  + 1.596*(c-128)))
#define MG(a,b,c) ( (1.164*(a-16)  - 0.813*(c-128) - 0.392*(b-128)))
#define MB(a,b,c) ( (1.164*(a-16)  + 2.017*(b-128)))

#define MY(r,g,b) ( ( r*  0.257  + g*  0.504  + b*  0.098 +  16))
#define MU(r,g,b) ( ( r*(-0.148) + g*(- 0.291) + b*  0.439 + 128))
#define MV(r,g,b) ( ( r*  0.439  + g*(-0.368) + b*(-0.071) + 128))
//大小判断
#define DY(a,b,c) (MY(a,b,c) > 255 ? 255 : (MY(a,b,c) < 0 ? 0 : MY(a,b,c)))
#define DU(a,b,c) (MU(a,b,c) > 255 ? 255 : (MU(a,b,c) < 0 ? 0 : MU(a,b,c)))
#define DV(a,b,c) (MV(a,b,c) > 255 ? 255 : (MV(a,b,c) < 0 ? 0 : MV(a,b,c)))

void Rgb2YuvConvert(unsigned char *RGB, unsigned char *YUV , int width, int height)
{
    //变量声明
    unsigned int i,x,y,j;
    unsigned char *Y = NULL;
    unsigned char *U = NULL;

    Y = YUV;
    U = YUV + width*height;

    for(y=0; y < height; y++)
        for(x=0; x < width; x++)
        {
            j = y*width + x;
            Y[j] = (unsigned char)(DY(RGB[j], RGB[j+width*height], RGB[j+width*height*2]));
            if(x%4 == 3)
            {
                U[y*width/2 + x/2 -1] = (unsigned char)
                       (((DU(RGB[j], RGB[j+width*height], RGB[j+width*height*2])) +
                         DU(RGB[j-1], RGB[j+width*height-1], RGB[j+width*height*2-1]) +
                         DU(RGB[j-2], RGB[j+width*height-2], RGB[j+width*height*2-2]) +
                         DU(RGB[j-3], RGB[j+width*height-3], RGB[j+width*height*2-3]))/4);
                //V
                U[y*width/2 + x/2] = (unsigned char)
                        (((DV(RGB[j], RGB[j+width*height], RGB[j+width*height*2])) +
                             DV(RGB[j-1], RGB[j+width*height-1], RGB[j+width*height*2-1]) +
                             DV(RGB[j-2], RGB[j+width*height-2], RGB[j+width*height*2-2]) +
                             DV(RGB[j-3], RGB[j+width*height-3], RGB[j+width*height*2-3]))/4);
            }
        }
}


//转换函数
void Yuv420ToRgbConvert(unsigned char* yuv, unsigned char* rgb, int width, int height)
{
    //变量声明
    int i = 0;
    int temp = 0;
    int ImgSize = width*height;
    unsigned char* cTemp[6];
    int uIndex;

    //转换指定帧  如果你不是处理文件 主要看这里
    cTemp[0] = yuv;                        //y分量地址
    cTemp[1] = yuv + ImgSize;            //u分量地址
//    cTemp[2] = cTemp[1] + (ImgSize>>2);    //v分量地址
    cTemp[3] = rgb;                        //r分量地址
    cTemp[4] = rgb + ImgSize;            //g分量地址
    cTemp[5] = cTemp[4] + ImgSize;        //b分量地址
    for(int y=0; y < height; y++){
        for(int x=0; x < width; x++)
            {
                uIndex = ((y*width+x)/4)*2;
                //r分量
                temp = MR(cTemp[0][y*width+x] , cTemp[1][uIndex], cTemp[1][uIndex + 1]);
                cTemp[3][y*width+x] = temp<0 ? 0 : (temp>255 ? 255 : temp);
                //g分量
                temp = MG(cTemp[0][y*width+x] , cTemp[1][uIndex], cTemp[1][uIndex + 1]);
                cTemp[4][y*width+x] = temp<0 ? 0 : (temp>255 ? 255 : temp);
                //b分量
                temp = MB(cTemp[0][y*width+x] , cTemp[1][uIndex], cTemp[1][uIndex + 1]);
                cTemp[5][y*width+x] = temp<0 ? 0 : (temp>255 ? 255 : temp);
            }
    }
}

void rgbTailor(unsigned char *src, int srcWidth, int srcHeight, unsigned char *dest, int destWidth, int destHeight, int startX, int startY){
    int srcFrameSize = srcHeight*srcWidth;
    int destFrameSize = destHeight*destWidth;
    for(int i=0; i<destHeight-startY; i++){
        for(int j=0; j<destWidth-startX; j++){
            dest[i*destWidth + j] = src[(startY+i)*srcWidth + j + startX];
            dest[i*destWidth + j + destFrameSize] = src[(startY+i)*srcWidth + j + startX + srcFrameSize];
            dest[i*destWidth + j + destFrameSize*2] = src[(startY+i)*srcWidth + j + startX + srcFrameSize*2];
        }
    }
}

void rgbCompress(unsigned char *src, int srcWidth, int srcHeight, unsigned char *dest, int destWidth, int destHeight){
    int srcFrameSize = srcHeight*srcWidth;
    int destFrameSize = destHeight*destWidth;

    int delX = srcWidth - destWidth;
    int delY = srcHeight - destHeight;

    int delXindex = srcWidth/delX;
    int delYindex = srcHeight/delY;

    for(int i=0,i2=0; i<srcHeight  && i2<destHeight; i++,i2++){
        if(i%delYindex == 0){
            i++;
        }
        for(int j=0,j2=0; j<srcWidth && j2<destWidth; j++, j2++){
             if(j%delXindex == 0){
               j++;
             }
            dest[i2*destWidth + j2] = src[i*srcWidth + j];
            dest[i2*destWidth + j2 + destFrameSize] = src[i*srcWidth + j + srcFrameSize];
            dest[i2*destWidth + j2 + destFrameSize*2] = src[i*srcWidth + j + srcFrameSize*2];
        }
    }
}

void test(float w,float h)
{
    LOGE("#########22222# %f, %f", w, h);
}


//////////////////////////////////////////////////////////////////////////
//放大或者缩小RGB24，采用双线性插值算法
//////////////////////////////////////////////////////////////////////////
void ZoomBitMap(unsigned char *pSrcImg, unsigned char *pDstImg, int nWidth, int nHeight, float fRateW,float fRateH)
{
//    LOGE("#########111111# %f, %f", fRateW, fRateH);
	int i = 0;
	int j = 0;

	float fX, fY;

	int iStepSrcImg = nWidth;
	int iStepDstImg = nWidth * fRateW;

	int srcImageSize = nWidth*nHeight;
	int destImageSize = srcImageSize*fRateW*fRateH;

	int iX, iY;

	unsigned char bUpLeft, bUpRight, bDownLeft, bDownRight;
	unsigned char gUpLeft, gUpRight, gDownLeft, gDownRight;
	unsigned char rUpLeft, rUpRight, rDownLeft, rDownRight;

	unsigned char b, g, r;

	for(i = 0; i < nHeight * fRateH; i++)
	{
		for(j = 0; j < nWidth * fRateW; j++)
		{

			fX = ((float)j) /fRateW;
			fY = ((float)i) /fRateH;

			iX = (int)fX;
			iY = (int)fY;

			rUpLeft  = pSrcImg[iY * iStepSrcImg  + iX  + 0];
			rUpRight = pSrcImg[iY * iStepSrcImg  + (iX + 1)  + 0];

			rDownLeft  = pSrcImg[(iY + 1) * iStepSrcImg  + iX  + 0];
			rDownRight = pSrcImg[(iY + 1) * iStepSrcImg  + (iX + 1)  + 0];

			gUpLeft  = pSrcImg[iY * iStepSrcImg + iX  + srcImageSize];
			gUpRight = pSrcImg[iY * iStepSrcImg + (iX + 1) + srcImageSize];

			gDownLeft  = pSrcImg[(iY + 1) * iStepSrcImg + iX + srcImageSize];
			gDownRight = pSrcImg[(iY + 1) * iStepSrcImg + (iX + 1) + srcImageSize];

			bUpLeft  = pSrcImg[iY * iStepSrcImg + iX + 2*srcImageSize];
			bUpRight = pSrcImg[iY * iStepSrcImg + (iX + 1) + 2*srcImageSize];

			bDownLeft  = pSrcImg[(iY + 1) * iStepSrcImg + iX + 2*srcImageSize];
			bDownRight = pSrcImg[(iY + 1) * iStepSrcImg + (iX + 1) + 2*srcImageSize];

			b = bUpLeft * (iX + 1 - fX) * (iY + 1 - fY) + bUpRight * (fX - iX) * (iY + 1 - fY)
				+ bDownLeft * (iX + 1 - fX) * (fY - iY) + bDownRight * (fX - iX) * (fY - iY);

			g = gUpLeft * (iX + 1 - fX) * (iY + 1 - fY) + gUpRight * (fX - iX) * (iY + 1 - fY)
				+ gDownLeft * (iX + 1 - fX) * (fY - iY) + gDownRight * (fX - iX) * (fY - iY);

			r = rUpLeft * (iX + 1 - fX) * (iY + 1 - fY) + rUpRight * (fX - iX) * (iY + 1 - fY)
				+ rDownLeft * (iX + 1 - fX) * (fY - iY) + rDownRight * (fX - iX) * (fY - iY);


//			if(iY >= 0 && iY <= nHeight * 2 && iX >= 0 && iX <= nWidth * 2)
//			{
				pDstImg[i * iStepDstImg + j + 0] = r;        //R
				pDstImg[i * iStepDstImg + j + 1*destImageSize] = g;        //G
				pDstImg[i * iStepDstImg + j + 2*destImageSize] = b;        //B
//			}
		}
	}
}