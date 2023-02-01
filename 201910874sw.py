#!/usr/bin/env python
# -*- coding: utf-8 -*-

import numpy as np
import cv2, random, math, time

Width = 640
Height = 480
Offset = 330
Gap = 60

#사각형 그리기
def draw_rectangle(img, lpos, rpos, offset=0):
    center = int((lpos+rpos)/2) #센터 위치 설정
    cv2.rectangle(img, (lpos-5, 15+offset), (lpos+5, 25+offset), (0,255,0), 2) #왼쪽 사각형
    cv2.rectangle(img, (rpos-5, 15+offset), (rpos+5, 25+offset), (0,255,0), 2) #오른쪽 사각형
    cv2.rectangle(img, (center-5,15+offset), (center+5,25+offset), (0,255,0), 2) #센터 사각형
    cv2.rectangle(img, (315,15+offset), (325,25+offset), (0,0,255), 2) #빨간색 사각형
    return img # 이미지 반환

#좌우 분리
def divide_left_right(lines):
    global Width
    low_slope_threshold = 0
    high_slope_threshold = 10 #기울기 임계값
    slopes = [] #기울기 모음
    new_lines = [] #선분 모음
    # 찾은 선분들의 모음에서 좌표 파악
    for line in lines:
        x1, y1, x2, y2 = line[0]
        # x2와 x1이 같다면
        if x2-x1==0:
            slope=0 # 기울기 : 0
        else:
            slope=float(y2-y1)/float(x2-x1) #아니라면 기울기 구하기
        # 기울기가 임계값 사이에 있으면
        if abs(slope)>low_slope_threshold and abs(slope)<high_slope_threshold:
            slopes.append(slope) # 기울기모음에 기울기 추가
            new_lines.append(line[0]) #선분 모음에 선분 추가
    left_lines=[] #왼쪽 선분
    right_lines=[] #오른쪽 선분
    #기울기 모음의 길이만큼 진행
    for j in range(len(slopes)):
        Line=new_lines[j] #선분과
        slope=slopes[j] #기울기 추출
        x1, y1, x2, y2=Line #좌표 추출
        #기울기:음수& x2가 넓이/2-90보다 작으면
        if (slope<0) and (x2<Width/2-90):
            left_lines.append([Line.tolist()]) #왼쪽 선분에 추가
        #기울기:양수& x2가 넓이/2-90보다 크면
        elif (slope>0) and (x1>Width/2+90):
            right_lines.append([Line.tolist()]) #오른쪽 선분에 추가
    return left_lines,right_lines

#여러 선분들을 하나의 선분으로 변환
def get_line_params(lines):
    x_sum=0.0 #x합
    y_sum=0.0 #y합
    m_sum=0.0 #m합
    size=len(lines) #선분모음의 길이만큼 size 설정
    #size가 0이면
    if size==0:
        return 0,0 #0,0 리턴
    #선분모음에서
    for line in lines:
        x1, y1, x2, y2 = line[0] #좌표추출
        x_sum += x1+x2 #x합에 x좌표 추가
        y_sum += y1+y2 #y합에 y좌표 추가
        m_sum += float(y2-y1)/float(x2-x1) #m합에 기울기 추가
    x_avg=float(x_sum)/float(size*2) #x평균 계산
    y_avg=float(y_sum)/float(size*2) #y평균 계산
    m=m_sum/size #m 평균 계산
    b=y_avg-m*x_avg #b 계산
    return m,b

#선분 위치 찾기
def get_line_pos(lines, left=False, right=False):
    global Width, Height
    global Offset, Gap
    m,b=get_line_params(lines) #하나의 선분으로 변환한 m,b값
    x1, x2 = 0,0
    #선분을 못찾았을때
    if m==0 and b==0:
        if left:
            pos=0 #왼쪽 위치 0
        if right:
            pos=Width #오른쪽 위치 Width만큼 (양끝)
    #아니면
    else:
        y=Gap/2 #y설정
        pos=(y-b)/m #pos 구하기
        b += Offset #선분 평행이동
        x1=(Height-b)/float(m) 
        x2=((Height/2)-b)/float(m) #평행이동한 선분을 늘려서 그려줌
    return x1, x2, int(pos)

#프레임 해석
def process_image(frame):
    global Width
    global Offset, Gap
    gray=cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY) #이미지 회색으로 변환
    kernel_size=5 #kernel_size 설정
    blur_gray=cv2.GaussianBlur(gray, (kernel_size, kernel_size), 0) #회색 이미지 흐릿하게 만들기
    low_threshold=60 
    high_threshold=70 #임계값 설정
    edge_img=cv2.Canny(np.uint8(blur_gray), low_threshold, high_threshold) #그림에서 외곽선 찾기
    roi=edge_img[Offset:Offset+Gap, 0:Width] #roi 설정
    all_lines=cv2.HoughLinesP(roi,1,math.pi/180,40,30,10) #그림에서 직선 검출
    # 직선이 없으면
    if all_lines is None:
        return (0,640),frame #좌우 포지션은 양끝
    left_lines,right_lines=divide_left_right(all_lines) #좌우 선분 찾기
    lx1,lx2,lpos=get_line_pos(left_lines,left=True) #왼쪽 선분위치 찾기
    rx1,rx2,rpos=get_line_pos(right_lines,right=True) #오른쪽 선분 위치 찾기
    frame=cv2.line(frame,(int(lx1),Height),(int(lx2),int(Height/2)),(255,0,0),3) #왼쪽 선분 그리기
    frame=cv2.line(frame,(int(rx1),Height),(int(rx2),int(Height/2)),(255,0,0),3) #오른쪽 선분 그리기
    frame=draw_rectangle(frame,lpos,rpos,offset=Offset) #사각형 그리기
    return (lpos,rpos),frame

# 핸들 그리기
def draw_steer(image,steer_angle):
    global Width,Height,arrow_pic
    arrow_pic=cv2.imread('steer_arrow.png',cv2.IMREAD_COLOR) #컬러로 그림 읽어오기
    origin_Height=arrow_pic.shape[0] #그림의 높이
    origin_Width=arrow_pic.shape[1] #넓이 측정
    steer_wheel_center=origin_Height*0.74 #center 찾기
    arrow_Height=Height/2 #화살표 높이
    arrow_Width=(arrow_Height*462)/728 #화살표 넓이
    matrix=cv2.getRotationMatrix2D((origin_Width/2,steer_wheel_center),(steer_angle)*1.5,0.7) #핸들 그림 회전시키기
    arrow_pic=cv2.warpAffine(arrow_pic,matrix,(origin_Width+60,origin_Height)) #이미지 기하학적 변환
    arrow_pic=cv2.resize(arrow_pic,dsize=(int(arrow_Width),int(arrow_Height)),interpolation=cv2.INTER_AREA) #이미지 리사이징
    gray_arrow=cv2.cvtColor(arrow_pic,cv2.COLOR_BGR2GRAY) #이미지 회색으로 만들기
    _,mask=cv2.threshold(gray_arrow,1,255,cv2.THRESH_BINARY_INV) #이미지 검정색, 흰색으로 만들기
    arrow_roi=image[int(arrow_Height):Height,int((Width/2-arrow_Width/2)):int((Width/2+arrow_Width/2))] #이미지 추출
    arrow_roi=cv2.resize(arrow_roi,dsize=(int(arrow_Width),int(arrow_Height)),interpolation=cv2.INTER_AREA) #추출 이미지 리사이징
    arrow_roi=cv2.add(arrow_pic,arrow_roi,mask=mask) #이미지 두개 합치기
    res=cv2.add(arrow_roi,arrow_pic) #이미지 두개 합치기
    image[int((Height-arrow_Height)):Height,int((Width/2-arrow_Width/2)):int((Width/2+arrow_Width/2))-1]=res #res에서 이미지 추출
    cv2.imshow('steer',image) #이미지 보여주기

#실행 코드
def start():
    global image,Width,Height
    cap=cv2.VideoCapture('kmutrack.mp4') #영상 프레임단위 캡쳐
    #잘 되었다면
    while cap.isOpened():
        ret, image=cap.read() #image 읽어들이기
        pos, frame=process_image(image) #image 해석
        center=(pos[0]+pos[1])/2 #center 찾기
        angle=320-center #angle 계산
        steer_angle=angle*0.4 #steer_angle 계산
        draw_steer(frame,steer_angle) #핸들 그리기
        if cv2.waitKey(3)&0xFF==ord('q'):
            break #q 누르면 멈추기
if __name__=='__main__':
    start() #

   
#https://tonylim.tistory.com/54
