import socket
import serial
import time

#소켓통신 주소
raspi_ip_address = "192.168.137.42"
raspi_port = 22222

#소켓통신 설정
server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_RCVBUF, 8192)

#시리얼 통신 설정
ardu_serial = serial.Serial('/dev/ttyACM0', 9600)

server_socket.bind((raspi_ip_address, raspi_port))


while True:
   #소켓 통신 대기 및 연결
   server_socket.listen(1)
   print("Waiting for a connection...")

   client_socket, client_address = server_socket.accept()
   print("Connected to:", client_address)

   #소켓 통신 데이터 수신
   received_data = client_socket.recv(1024)  
   received_data = received_data.decode('utf-8')
   
   #시리얼 데이터 전송
   ardu_serial.write(received_data.encode())
   
   #수신한 데이터 출력
   print("Received data:", received_data)
   
   time.sleep(0.3)

   
#통신 종료
client_socket.close()
ardu_serial.close()
server_socket.close()
