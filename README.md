<img src="https://user-images.githubusercontent.com/43338817/118908353-3eff9880-b95c-11eb-82f5-de2868e3ae4e.png" alt="" data-canonical-src="https://user-images.githubusercontent.com/43338817/118908353-3eff9880-b95c-11eb-82f5-de2868e3ae4e.png" width="250" height="250" /> <img src="https://user-images.githubusercontent.com/43338817/118910199-0c0ad400-b95f-11eb-8165-c469394fa8ab.png" alt="" data-canonical-src="https://user-images.githubusercontent.com/43338817/118910199-0c0ad400-b95f-11eb-8165-c469394fa8ab.png" width="250" height="250" />

# 예제 -  호텔예약서비스

- 체크포인트 : https://workflowy.com/s/assessment-check-po/T5YrzcMewfo4J6LW


# Table of contents

- [예제 - 호텔예약서비스](#---)
  - [서비스 시나리오](#서비스-시나리오)
  - [체크포인트](#체크포인트)
  - [분석/설계](#분석설계)
  - [구현:](#구현-)
    - [DDD 의 적용](#ddd-의-적용)
    - [폴리글랏 퍼시스턴스](#폴리글랏-퍼시스턴스)
    - [폴리글랏 프로그래밍](#폴리글랏-프로그래밍)
    - [동기식 호출 과 Fallback 처리](#동기식-호출-과-Fallback-처리)
    - [비동기식 호출 과 Eventual Consistency](#비동기식-호출-과-Eventual-Consistency)
  - [운영](#운영)
    - [CI/CD 설정](#cicd설정)
    - [동기식 호출 / 서킷 브레이킹 / 장애격리](#동기식-호출-서킷-브레이킹-장애격리)
    - [오토스케일 아웃](#오토스케일-아웃)
    - [무정지 재배포](#무정지-재배포)
    - [ConfigMap 사용](#ConfigMap-사용)
    - [Self-healing (Liveness Probe)](#Self-healing-Liveness-Probe)
    - [CQRS](#CQRS)
  - [신규 개발 조직의 추가](#신규-개발-조직의-추가)

# 서비스 시나리오

호텔 예약 서비스 따라하기

기능적 요구사항
1. 호스트가 룸을 등록한다.
1. 호스트가 룸을 삭제한다.
3. 게스트가 룸을 검색한다.
4. 게스트가 룸을 선택하여 사용 예약한다.
5. 게스트가 결제한다. (Sync, 결제서비스)
6. 결제가 완료되면, 결제 & 예약 내용을 게스트에게 알림을 전송한다. (Async, 알림서비스)
7. 예약 내역을 호스트에게 전달한다.
8. 게스트는 본인의 예약 내용 및 상태를 조회한다.
9. 게스트는 본인의 예약을 취소할 수 있다.
12. 예약이 취소되면, 결제를 취소한다. (Async, 결제서비스)
13. 결제가 취소되면, 결제 취소 내용을 게스트에게 알림을 전송한다. (Async, 알림서비스)
14. 룸이 삭제 된다면, 예약도 취소된다.
15. 룸 삭제, 생성 시 호스트에게 알림을 전송한다. (Async, 알림서비스)


비기능적 요구사항
1. 트랜잭션
    1. 결제가 되지 않은 예약건은 아예 거래가 성립되지 않아야 한다 - Sync 호출 
1. 장애격리
    1. 통지(알림) 기능이 수행되지 않더라도 예약은 365일 24시간 받을 수 있어야 한다 - Async (event-driven), Eventual Consistency
    1. 결제시스템이 과중되면 사용자를 잠시동안 받지 않고 결제를 잠시후에 하도록 유도한다 - Circuit breaker, fallback
1. 성능
    1. 게스트와 호스트가 자주 예약관리에서 확인할 수 있는 상태를 마이페이지(프론트엔드)에서 확인할 수 있어야 한다 - CQRS
    1. 처리상태가 바뀔때마다 email, app push 등으로 알림을 줄 수 있어야 한다 - Event driven



# 체크포인트

- 분석 설계


  - 이벤트스토밍: 
    - 스티커 색상별 객체의 의미를 제대로 이해하여 헥사고날 아키텍처와의 연계 설계에 적절히 반영하고 있는가?
    - 각 도메인 이벤트가 의미있는 수준으로 정의되었는가?
    - 어그리게잇: Command와 Event 들을 ACID 트랜잭션 단위의 Aggregate 로 제대로 묶었는가?
    - 기능적 요구사항과 비기능적 요구사항을 누락 없이 반영하였는가?    

  - 서브 도메인, 바운디드 컨텍스트 분리
    - 팀별 KPI 와 관심사, 상이한 배포주기 등에 따른  Sub-domain 이나 Bounded Context 를 적절히 분리하였고 그 분리 기준의 합리성이 충분히 설명되는가?
      - 적어도 3개 이상 서비스 분리
    - 폴리글랏 설계: 각 마이크로 서비스들의 구현 목표와 기능 특성에 따른 각자의 기술 Stack 과 저장소 구조를 다양하게 채택하여 설계하였는가?
    - 서비스 시나리오 중 ACID 트랜잭션이 크리티컬한 Use 케이스에 대하여 무리하게 서비스가 과다하게 조밀히 분리되지 않았는가?
  - 컨텍스트 매핑 / 이벤트 드리븐 아키텍처 
    - 업무 중요성과  도메인간 서열을 구분할 수 있는가? (Core, Supporting, General Domain)
    - Request-Response 방식과 이벤트 드리븐 방식을 구분하여 설계할 수 있는가?
    - 장애격리: 서포팅 서비스를 제거 하여도 기존 서비스에 영향이 없도록 설계하였는가?
    - 신규 서비스를 추가 하였을때 기존 서비스의 데이터베이스에 영향이 없도록 설계(열려있는 아키택처)할 수 있는가?
    - 이벤트와 폴리시를 연결하기 위한 Correlation-key 연결을 제대로 설계하였는가?

  - 헥사고날 아키텍처
    - 설계 결과에 따른 헥사고날 아키텍처 다이어그램을 제대로 그렸는가?
    
- 구현
  - [DDD] 분석단계에서의 스티커별 색상과 헥사고날 아키텍처에 따라 구현체가 매핑되게 개발되었는가?
    - Entity Pattern 과 Repository Pattern 을 적용하여 JPA 를 통하여 데이터 접근 어댑터를 개발하였는가
    - [헥사고날 아키텍처] REST Inbound adaptor 이외에 gRPC 등의 Inbound Adaptor 를 추가함에 있어서 도메인 모델의 손상을 주지 않고 새로운 프로토콜에 기존 구현체를 적응시킬 수 있는가?
    - 분석단계에서의 유비쿼터스 랭귀지 (업무현장에서 쓰는 용어) 를 사용하여 소스코드가 서술되었는가?
  - Request-Response 방식의 서비스 중심 아키텍처 구현
    - 마이크로 서비스간 Request-Response 호출에 있어 대상 서비스를 어떠한 방식으로 찾아서 호출 하였는가? (Service Discovery, REST, FeignClient)
    - 서킷브레이커를 통하여  장애를 격리시킬 수 있는가?
  - 이벤트 드리븐 아키텍처의 구현
    - 카프카를 이용하여 PubSub 으로 하나 이상의 서비스가 연동되었는가?
    - Correlation-key:  각 이벤트 건 (메시지)가 어떠한 폴리시를 처리할때 어떤 건에 연결된 처리건인지를 구별하기 위한 Correlation-key 연결을 제대로 구현 하였는가?
    - Message Consumer 마이크로서비스가 장애상황에서 수신받지 못했던 기존 이벤트들을 다시 수신받아 처리하는가?
    - Scaling-out: Message Consumer 마이크로서비스의 Replica 를 추가했을때 중복없이 이벤트를 수신할 수 있는가
    - CQRS: Materialized View 를 구현하여, 타 마이크로서비스의 데이터 원본에 접근없이(Composite 서비스나 조인SQL 등 없이) 도 내 서비스의 화면 구성과 잦은 조회가 가능한가?

  - 폴리글랏 플로그래밍
    - 각 마이크로 서비스들이 하나이상의 각자의 기술 Stack 으로 구성되었는가?
    - 각 마이크로 서비스들이 각자의 저장소 구조를 자율적으로 채택하고 각자의 저장소 유형 (RDB, NoSQL, File System 등)을 선택하여 구현하였는가?
  - API 게이트웨이
    - API GW를 통하여 마이크로 서비스들의 집입점을 통일할 수 있는가?
    - 게이트웨이와 인증서버(OAuth), JWT 토큰 인증을 통하여 마이크로서비스들을 보호할 수 있는가?
- 운영
  - SLA 준수
    - 셀프힐링: Liveness Probe 를 통하여 어떠한 서비스의 health 상태가 지속적으로 저하됨에 따라 어떠한 임계치에서 pod 가 재생되는 것을 증명할 수 있는가?
    - 서킷브레이커, 레이트리밋 등을 통한 장애격리와 성능효율을 높힐 수 있는가?
    - 오토스케일러 (HPA) 를 설정하여 확장적 운영이 가능한가?
    - 모니터링, 앨럿팅: 
  - 무정지 운영 CI/CD (10)
    - Readiness Probe 의 설정과 Rolling update을 통하여 신규 버전이 완전히 서비스를 받을 수 있는 상태일때 신규버전의 서비스로 전환됨을 siege 등으로 증명 
    - Contract Test :  자동화된 경계 테스트를 통하여 구현 오류나 API 계약위반를 미리 차단 가능한가?


# 분석/설계


## AS-IS 조직 (Horizontally-Aligned)
  ![image](https://user-images.githubusercontent.com/487999/79684144-2a893200-826a-11ea-9a01-79927d3a0107.png)

## TO-BE 조직 (Vertically-Aligned)
  ![image](https://user-images.githubusercontent.com/487999/79684159-3543c700-826a-11ea-8d5f-a3fc0c4cad87.png)


## Event Storming 결과
* MSAEz 로 모델링한 이벤트스토밍 결과:  http://www.msaez.io/#/storming/Bb0WFdkafROy3qiBkH258FKxOLW2/share/8efe31263c3025004aaa3da86965bf52


### 이벤트 도출
![image](https://user-images.githubusercontent.com/45786659/118963296-42694300-b9a1-11eb-9e67-05148a338fab.png)

### 부적격 이벤트 탈락
![image](https://user-images.githubusercontent.com/45786659/118963312-46956080-b9a1-11eb-9600-f6868b1672f8.png)

    - 과정중 도출된 잘못된 도메인 이벤트들을 걸러내는 작업을 수행함
        - 룸검색됨, 예약정보조회됨 :  UI 의 이벤트이지, 업무적인 의미의 이벤트가 아니라서 제외

### 액터, 커맨드 부착하여 읽기 좋게
![image](https://user-images.githubusercontent.com/45786659/118963330-4bf2ab00-b9a1-11eb-81b2-f44fc65f1cca.png)

### 어그리게잇으로 묶기
![image](https://user-images.githubusercontent.com/45786659/118963371-59a83080-b9a1-11eb-9ade-4556d1fb4cc8.png)

    - 룸, 예약관리, 결제는 그와 연결된 command 와 event 들에 의하여 트랜잭션이 유지되어야 하는 단위로 그들 끼리 묶어줌

### 바운디드 컨텍스트로 묶기

![image](https://user-images.githubusercontent.com/45786659/118963396-5f057b00-b9a1-11eb-9289-909a85d38d82.png)

    - 도메인 서열 분리 
        - Core Domain:  룸, 예약 : 없어서는 안될 핵심 서비스이며, 연견 Up-time SLA 수준을 99.999% 목표, 배포주기는 1주일 1회 미만
        - Supporting Domain:   알림, 마이페이지 : 경쟁력을 내기위한 서비스이며, SLA 수준은 연간 60% 이상 uptime 목표, 배포주기는 각 팀의 자율이나 표준 스프린트 주기가 1주일 이므로 1주일 1회 이상을 기준으로 함.
        - General Domain:   결제 : 결제서비스로 3rd Party 외부 서비스를 사용하는 것이 경쟁력이 높음 (핑크색으로 이후 전환할 예정)

### 폴리시 부착 (괄호는 수행주체, 폴리시 부착을 둘째단계에서 해놔도 상관 없음. 전체 연계가 초기에 드러남)

![image](https://user-images.githubusercontent.com/45786659/118963431-64fb5c00-b9a1-11eb-9064-e3a5dbebdd34.png)

### 폴리시의 이동과 컨텍스트 매핑 (점선은 Pub/Sub, 실선은 Req/Resp)

![image](https://user-images.githubusercontent.com/45786659/118963450-6a58a680-b9a1-11eb-9ee3-0a37983b6081.png)
    
### 호텔 바운디드 컨텍스트 내 PUB/SUB 고려하여 모델 간소화

![image](https://github.com/juwarny/example-hotel-booking/blob/master/%E1%84%89%E1%85%B3%E1%84%8F%E1%85%B3%E1%84%85%E1%85%B5%E1%86%AB%E1%84%89%E1%85%A3%E1%86%BA%202021-06-04%20%E1%84%8B%E1%85%A9%E1%84%8C%E1%85%A5%E1%86%AB%207.29.59.png)
### 기능적/비기능적 요구사항을 커버하는지 검증

![image](https://github.com/juwarny/example-hotel-booking/blob/master/%E1%84%89%E1%85%B3%E1%84%8F%E1%85%B3%E1%84%85%E1%85%B5%E1%86%AB%E1%84%89%E1%85%A3%E1%86%BA%202021-06-04%20%E1%84%8B%E1%85%A9%E1%84%8C%E1%85%A5%E1%86%AB%207.29.59.png)

    - (ok) 호스트가 룸을 등록한다.
    - (ok) 호스트가 룸을 삭제한다.
    - (ok) 호스트가 롬을 삭제 시 예약 취소
    - (ok) 각 이벤트가 알림으로 전달 (Async, 알림 서비스)

    - (ok) 게스트가 룸을 검색한다.
    - (ok) 게스트가 룸을 선택하여 사용 예약한다.
    - (ok) 게스트가 결제한다. (Sync, 결제서비스)
    - (ok) 결제가 완료되면, 결제 & 예약 내용을 게스트에게 알림을 전송한다. (Async, 알림서비스)
    - (ok) 예약 내역을 호스트에게 전달한다.
    
    - (ok) 게스트는 본인의 예약 내용 및 상태를 조회한다.
    - (ok) 게스트는 본인의 예약을 취소할 수 있다.
    - (ok) 예약이 취소되면, 결제를 취소한다. (Async, 결제서비스)
    - (ok) 결제가 취소되면, 결제 취소 내용을 게스트에게 알림을 전송한다. (Async, 알림서비스)

### 비기능 요구사항에 대한 검증

![image](https://github.com/juwarny/example-hotel-booking/blob/master/%E1%84%89%E1%85%B3%E1%84%8F%E1%85%B3%E1%84%85%E1%85%B5%E1%86%AB%E1%84%89%E1%85%A3%E1%86%BA%202021-06-04%20%E1%84%8B%E1%85%A9%E1%84%8C%E1%85%A5%E1%86%AB%207.29.59.png)

    - 마이크로 서비스를 넘나드는 시나리오에 대한 트랜잭션 처리
        - 룸 예약시 결제처리: 결제가 완료되지 않은 예약은 절대 받지 않는다에 따라, ACID 트랜잭션 적용. 예약 완료시 결제처리에 대해서는 Request-Response 방식 처리
        - 예약 완료시 알림 처리: 예약에서 알림 마이크로서비스로 예약 완료 내용이 전달되는 과정에 있어서 알림 마이크로서비스가 별도의 배포주기를 가지기 때문에 Eventual Consistency 방식으로 트랜잭션 처리함.
        - 나머지 모든 inter-microservice 트랜잭션: 예약상태, 예약취소 등 모든 이벤트에 대해 알림 처리하는 등, 데이터 일관성의 시점이 크리티컬하지 않은 모든 경우가 대부분이라 판단, Eventual Consistency 를 기본으로 채택함.


## 헥사고날 아키텍처 다이어그램 도출
    
![image](https://user-images.githubusercontent.com/45786659/118924241-c4dd0d00-b977-11eb-94b2-c49d3b4e3de5.png)


    - Chris Richardson, MSA Patterns 참고하여 Inbound adaptor와 Outbound adaptor를 구분함
    - 호출관계에서 PubSub 과 Req/Resp 를 구분함
    - 서브 도메인과 바운디드 컨텍스트의 분리:  각 팀의 KPI 별로 아래와 같이 관심 구현 스토리를 나눠가짐


# 구현:

분석/설계 단계에서 도출된 헥사고날 아키텍처에 따라, 각 BC별로 대변되는 마이크로 서비스들을 스프링부트로 구현하였다. 배포는 아래와 같이 수행한다.

```
# eks cluster 생성
eksctl create cluster --name user04-eks --version 1.17 --nodegroup-name standard-workers --node-type t3.medium --nodes 4 --nodes-min 1 --nodes-max 4

# eks cluster 설정
aws eks --region ap-northeast-2 update-kubeconfig --name user04-eks
kubectl config current-context

# metric server 설치
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/download/v0.3.6/components.yaml

# Helm 설치
curl https://raw.githubusercontent.com/helm/helm/master/scripts/get-helm-3 > get_helm.sh
chmod 700 get_helm.sh
./get_helm.sh
(Helm 에게 권한을 부여하고 초기화)
kubectl --namespace kube-system create sa tiller
kubectl create clusterrolebinding tiller --clusterrole cluster-admin --serviceaccount=kube-system:tiller
helm init --service-account tiller

# Kafka 설치
helm repo update
helm repo add bitnami https://charts.bitnami.com/bitnami
kubectl create ns kafka
helm install my-kafka bitnami/kafka --namespace kafka

# myhotel namespace 생성
kubectl create namespace myhotel

# myhotel image build & push
cd myhotel/book
mvn package
docker build -t 740569282574.dkr.ecr.ap-northeast-1.amazonaws.com/book:latest .
docker push 740569282574.dkr.ecr.ap-northeast-1.amazonaws.com/book:latest

# myhotel deploy
cd myhotel/yaml
kubectl apply -f configmap.yaml
kubectl apply -f gateway.yaml
kubectl apply -f room.yaml
kubectl apply -f booking.yaml
kubectl apply -f pay.yaml
kubectl apply -f mypage.yaml
kubectl apply -f notification.yaml
kubectl apply -f siege.yaml
```

현황
```
#kubectl egt all -n myhotel

NAME                                READY   STATUS    RESTARTS   AGE
pod/booking-7dc6fbb847-psm2v        2/2     Running   0          10h
pod/gateway-cfc98454b-zkkz6         2/2     Running   0          14h
pod/mypage-9c855fddf-pxcgv          2/2     Running   0          10h
pod/notification-64fdcd86f5-pbhhk   2/2     Running   0          10h
pod/pay-b7b4c648b-p58cv             2/2     Running   0          10h
pod/room-7ffb788f5f-4xwmk           2/2     Running   0          10h
pod/siege                           2/2     Running   0          14h

NAME                   TYPE           CLUSTER-IP       EXTERNAL-IP                                                                   PORT(S)          AGE
service/booking        ClusterIP      10.100.170.242   <none>                                                                        8080/TCP         10h
service/gateway        LoadBalancer   10.100.250.208   a60ac02ae722f4bbf8b8a84c1ce84d8f-105579122.ap-northeast-2.elb.amazonaws.com   8080:31711/TCP   14h
service/mypage         ClusterIP      10.100.224.136   <none>                                                                        8080/TCP         10h
service/notification   ClusterIP      10.100.140.134   <none>                                                                        8080/TCP         10h
service/pay            ClusterIP      10.100.109.210   <none>                                                                        8080/TCP         10h
service/room           ClusterIP      10.100.108.119   <none>                                                                        8080/TCP         10h

NAME                           READY   UP-TO-DATE   AVAILABLE   AGE
deployment.apps/booking        1/1     1            1           10h
deployment.apps/gateway        1/1     1            1           14h
deployment.apps/mypage         1/1     1            1           10h
deployment.apps/notification   1/1     1            1           10h
deployment.apps/pay            1/1     1            1           10h
deployment.apps/room           1/1     1            1           10h

NAME                                      DESIRED   CURRENT   READY   AGE
replicaset.apps/booking-7dc6fbb847        1         1         1       10h
replicaset.apps/gateway-cfc98454b         1         1         1       14h
replicaset.apps/mypage-9c855fddf          1         1         1       10h
replicaset.apps/notification-64fdcd86f5   1         1         1       10h
replicaset.apps/pay-b7b4c648b             1         1         1       10h
replicaset.apps/room-7ffb788f5f           1         1         1       10h
```


## DDD 의 적용

- 각 서비스내에 도출된 핵심 Aggregate Root 객체를 Entity 로 선언하였다: (예시는 book 마이크로 서비스). 이때 가능한 현업에서 사용하는 언어 (유비쿼터스 랭귀지)를 그대로 사용하려고 노력했다. 

```
package myhotel;

import javax.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import myhotel.external.PayStatus;
import myhotel.external.Payment;
import myhotel.external.PaymentService;
import org.springframework.beans.BeanUtils;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="Booking_table")
public class Booking {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private Date startDate;
    private Date endDate;
    private Long guestId;
    private Long hostId;
    private Long roomId;
    private BookStatus status;
    private Integer price;


    ...

}


```
- Entity Pattern 과 Repository Pattern 을 적용하여 JPA 를 통하여 다양한 데이터소스 유형 (RDB or NoSQL) 에 대한 별도의 처리가 없도록 데이터 접근 어댑터를 자동 생성하기 위하여 Spring Data REST 의 RestRepository 를 적용하였다
```
package myhotel;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

@RepositoryRestResource(collectionResourceRel="bookings", path="bookings")
public interface BookingRepository extends PagingAndSortingRepository<Booking, Long>{
    List<Booking> findByRoomId(Long id);

}
```
- 적용 후 REST API 의 테스트
```
# 룸 등록처리
http POST http://room:8080/rooms price=1500 hostId=1

# 예약처리
http POST http://book:8080/books startDate="2012-04-23T18:25:43.511+0000" endDate="2012-04-27T18:25:43.511+0000" guestId=1 hostId=1 roomId=2 price=1000

# 예약 상태 확인
http http://book:8080/books/1

```


## 폴리글랏 퍼시스턴스
booking에 mysql를 연결 나머지 앱들은 h2를 사용

mysql-deployment.yaml

```
apiVersion: v1
kind: Service
metadata:
  name: mysql-svc
  labels:
    app: booking
spec:
  ports:
    - port: 3306
  selector:
    app: booking
    tier: mysql
  clusterIP: None
---
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: mysql-pv-claim
  labels:
    app: booking
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 100Mi
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: booking-mysql
  labels:
    app: booking
spec:
  selector:
    matchLabels:
      app: booking
      tier: mysql
  strategy:
    type: Recreate
  template:
    metadata:
      labels:
        app: booking
        tier: mysql
    spec:
      containers:
      - image: mysql:5.6
        name: mysql
        env:
        - name: MYSQL_ROOT_PASSWORD
          valueFrom:
            secretKeyRef:
              name: mysql-secret
              key: root-password
        - name: MYSQL_DATABASE
          valueFrom:
            secretKeyRef:
              name: mysql-secret
              key: database-name
        - name: MYSQL_USER
          valueFrom:
            secretKeyRef:
              name: mysql-secret
              key: user-username
        - name: MYSQL_PASSWORD
          valueFrom:
            secretKeyRef:
              name: mysql-secret
              key: user-password
        ports:
        - containerPort: 3306
          name: mysql
        volumeMounts:
        - name: mysql-persistent-storage
          mountPath: /var/lib/mysql
      volumes:
      - name: mysql-persistent-storage
        persistentVolumeClaim:
          claimName: mysql-pv-claim
```

secrets.yaml(base64)
```
apiVersion: v1
kind: Secret
data:
  root-password: 여기선 생략
  database-name: 여기선 생략
  user-username: 여기선 생략
  user-password: 여기선 생략
metadata:
  name: mysql-secret
```
application.yaml(booking)

```
datasource:
    url: jdbc:mysql://mysql-svc:3306/${DB_NAME}?useSSL=false
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    properties:
      hibernate:
        show_sql: true
        format_sql: true
    hibernate:
      ddl-auto: update
    databasePlatform: "org.hibernate.dialect.MySQL5InnoDBDialect"
```

![image](https://user-images.githubusercontent.com/18213483/120744127-50889900-c535-11eb-8ae3-79bec1a77e6f.png)


## 동기식 호출 과 Fallback 처리

분석단계에서의 조건 중 하나로 예약(book)->결제(pay) 간의 호출은 동기식 일관성을 유지하는 트랜잭션으로 처리하기로 하였다. 호출 프로토콜은 이미 앞서 Rest Repository 에 의해 노출되어있는 REST 서비스를 FeignClient 를 이용하여 호출하도록 한다. 

- 결제 서비스를 호출하기 위하여 Stub과 (FeignClient) 를 이용하여 Service 대행 인터페이스 (Proxy) 를 구현 

```
# (book) PaymentService.java 

@FeignClient(name="pay", url="http://pay:8080")
public interface PaymentService {

    @RequestMapping(method= RequestMethod.GET, path="/payments")
    public void pay(@RequestBody Payment payment);

}
```

- 예약을 받은 직후(@PostPersist) 결제를 요청하도록 처리
```
@Entity
@Table(name="Book_table")
public class Book {
    
    ...

     @PostPersist
    public void onPostPersist(){

        Payment payment = Payment.builder()
                .bookId(getId())
                .roomId(getRoomId())
                .guestId(getGuestId())
                .price(getPrice())
                .hostId(getHostId())
                .startDate(getStartDate())
                .endDate(getEndDate())
                .status(PayStatus.APPROVED)
                .build();

        // mappings goes here
        try {
            BookingApplication
                    .applicationContext
                    .getBean(PaymentService.class)
                    .pay(payment);
        }catch(Exception e) {
            throw new RuntimeException("결제서비스 호출 실패입니다.");
        }

        // 결제까지 완료되면 최종적으로 예약 완료 이벤트 발생
        Booked booked = new Booked();
        BeanUtils.copyProperties(this, booked);
        booked.publishAfterCommit();
    }
```

- 동기식 호출로 연결되어 있는 예약(book)->결제(pay) 간의 연결 상황을 Kiali Graph로 확인한 결과 (siege 이용하여 book POST)

![image](https://github.com/juwarny/example-hotel-booking/blob/master/%E1%84%89%E1%85%B3%E1%84%8F%E1%85%B3%E1%84%85%E1%85%B5%E1%86%AB%E1%84%89%E1%85%A3%E1%86%BA%202021-06-04%20%E1%84%8B%E1%85%A9%E1%84%8C%E1%85%A5%E1%86%AB%208.18.31.png)

- 동기식 호출에서는 호출 시간에 따른 타임 커플링이 발생하며, 결제 시스템이 장애가 나면 주문도 못받는다는 것을 확인:


```
# 결제 서비스를 잠시 내려놓음
cd yaml
$ kubectl delete -f pay.yaml
```

```
(base) juwonkim@JUWONui-MacBookAir yaml % kubectl get all -n myhotel
NAME                                READY   STATUS    RESTARTS   AGE
pod/booking-7dc6fbb847-psm2v        2/2     Running   0          11h
pod/gateway-cfc98454b-zkkz6         2/2     Running   0          15h
pod/mypage-9c855fddf-pxcgv          2/2     Running   0          11h
pod/notification-64fdcd86f5-pbhhk   2/2     Running   0          11h
pod/room-7ffb788f5f-4xwmk           2/2     Running   0          11h
pod/siege                           2/2     Running   0          15h

NAME                   TYPE           CLUSTER-IP       EXTERNAL-IP                                                                   PORT(S)          AGE
service/booking        ClusterIP      10.100.170.242   <none>                                                                        8080/TCP         11h
service/gateway        LoadBalancer   10.100.250.208   a60ac02ae722f4bbf8b8a84c1ce84d8f-105579122.ap-northeast-2.elb.amazonaws.com   8080:31711/TCP   15h
service/mypage         ClusterIP      10.100.224.136   <none>                                                                        8080/TCP         11h
service/notification   ClusterIP      10.100.140.134   <none>                                                                        8080/TCP         11h
service/room           ClusterIP      10.100.108.119   <none>                                                                        8080/TCP         11h

NAME                           READY   UP-TO-DATE   AVAILABLE   AGE
deployment.apps/booking        1/1     1            1           11h
deployment.apps/gateway        1/1     1            1           15h
deployment.apps/mypage         1/1     1            1           11h
deployment.apps/notification   1/1     1            1           11h
deployment.apps/room           1/1     1            1           11h

NAME                                      DESIRED   CURRENT   READY   AGE
replicaset.apps/booking-7dc6fbb847        1         1         1       11h
replicaset.apps/gateway-cfc98454b         1         1         1       15h
replicaset.apps/mypage-9c855fddf          1         1         1       11h
replicaset.apps/notification-64fdcd86f5   1         1         1       11h
replicaset.apps/room-7ffb788f5f           1         1         1       11h
```

```
# 예약처리 (siege 사용)
http POST http://book:8080/books startDate="2012-04-23T18:25:43.511+0000" endDate="2012-04-27T18:25:43.511+0000" guestId=1 hostId=1 roomId=2 price=1000  #Fail
```

```
{
    "timestamp": "2021-06-03T23:26:03.234+0000",
    "status": 500,
    "error": "Internal Server Error",
    "message": "Could not commit JPA transaction; nested exception is javax.persistence.RollbackException: Error while committing the transaction",
    "path": "/bookings"
}
```

```
# 결제서비스 재기동
$ kubectl apply -f pay.yaml
```

```
# 예약처리 (siege 사용)
http POST http://book:8080/books startDate="2012-04-23T18:25:43.511+0000" endDate="2012-04-27T18:25:43.511+0000" guestId=1 hostId=1 roomId=2 price=1000  #Success
```

```
{
    "startDate": "2012-04-23T18:25:43.511+0000",
    "endDate": "2012-04-27T18:25:43.511+0000",
    "guestId": 1,
    "hostId": 1,
    "roomId": 2,
    "status": "BOOKED",
    "price": 1000,
    "_links": {
        "self": {
            "href": "http://booking:8080/bookings/4"
        },
        "booking": {
            "href": "http://booking:8080/bookings/4"
        }
    }
}
```


- 또한 과도한 요청시에 서비스 장애가 도미노 처럼 벌어질 수 있다. (서킷브레이커, 폴백 처리는 운영단계에서 설명한다.)




## 비동기식 호출 / 시간적 디커플링 / 장애격리 / 최종 (Eventual) 일관성 테스트


결제가 이루어진 후에 알림 처리는 동기식이 아니라 비 동기식으로 처리하여 알림 시스템의 처리를 위하여 예약 블로킹 되지 않아도록 처리한다.
 
- 이를 위하여 예약관리, 결제이력에 기록을 남긴 후에 곧바로 결제승인이 되었다는 도메인 이벤트를 카프카로 송출한다(Publish)
 
```
@Entity
@Table(name="Payment_table")
public class Payment {

    ...

    @PostPersist
    public void onPostPersist(){
        PaymentApproved paymentApproved = new PaymentApproved();
        BeanUtils.copyProperties(this, paymentApproved);
        paymentApproved.publishAfterCommit();


    }

}
```
- 알림 서비스에서는 예약완료, 결제승인 이벤트에 대해서 이를 수신하여 자신의 정책을 처리하도록 PolicyHandler를 구현한다:

```
@Log
@Service
public class PolicyHandler{
    @Autowired NotificationRepository notificationRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPaymentApproved_Notify(@Payload PaymentApproved paymentApproved){

        if(!paymentApproved.validate()) return;
        saveMessageAndNotification(paymentApproved.getGuestId(), paymentApproved.toJson());
            
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPaymentCanceled_Notify(@Payload PaymentCanceled paymentCanceled){

        if(!paymentCanceled.validate()) return;
        saveMessageAndNotification(paymentCanceled.getGuestId(), paymentCanceled.toJson());
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverBookCanceled_Notify(@Payload BookCanceled bookCanceled){

        if(!bookCanceled.validate()) return;
        saveMessageAndNotification(bookCanceled.getGuestId(), bookCanceled.toJson());
            
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverBooked_Notify(@Payload Booked booked){

        if(!booked.validate()) return;
        saveMessageAndNotification(booked.getGuestId(), booked.toJson());
            
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverRegisteredRoom_Notify(@Payload RegisteredRoom registeredRoom){

        if(!registeredRoom.validate()) return;
        saveMessageAndNotification(registeredRoom.getHostId(), registeredRoom.toJson());
            
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverDeletedRoom_Notify(@Payload DeletedRoom deletedRoom){

        if(!deletedRoom.validate()) return;
        saveMessageAndNotification(deletedRoom.getHostId(), deletedRoom.toJson());
            
    }

    public void saveMessageAndNotification(Long userid, String message){
        Notification notification = new Notification();
        notification.setUserid(userid);
        notification.setMessage(
                "\n\n##### Notify : " + message + "\n\n"
        );

        log.info(notification.getMessage());
        notificationRepository.save(notification);
    }

    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}
```

알림 시스템은 예약/결제와 완전히 분리되어있으며, 이벤트 수신에 따라 처리되기 때문에, 알림 시스템이 유지보수로 인해 잠시 내려간 상태라도 예약을 받는데 문제가 없다:
```
# 알림 서비스를 잠시 내려놓음
cd yaml
kubectl delete -f notification.yaml
```

```
(base) juwonkim@JUWONui-MacBookAir yaml % kubectl get all -n myhotel         
NAME                           READY   STATUS    RESTARTS   AGE
pod/booking-7dc6fbb847-psm2v   2/2     Running   0          11h
pod/gateway-cfc98454b-zkkz6    2/2     Running   0          15h
pod/mypage-9c855fddf-pxcgv     2/2     Running   0          11h
pod/pay-b7b4c648b-8djqf        2/2     Running   0          8m19s
pod/room-7ffb788f5f-4xwmk      2/2     Running   0          11h
pod/siege                      2/2     Running   0          15h
```

```
# 예약처리 (siege 사용)
http POST http://book:8080/books startDate="2012-04-23T18:25:43.511+0000" endDate="2012-04-27T18:25:43.511+0000" guestId=1 hostId=1 roomId=3 price=1500  #Success
```

```
{
    "startDate": "2012-04-23T18:25:43.511+0000",
    "endDate": "2012-04-27T18:25:43.511+0000",
    "guestId": 1,
    "hostId": 1,
    "roomId": 3,
    "status": "BOOKED",
    "price": 1500,
    "_links": {
        "self": {
            "href": "http://booking:8080/bookings/5"
        },
        "booking": {
            "href": "http://booking:8080/bookings/5"
        }
    }
}
```

```
# 알림이력 확인 (siege 사용)
http http://alarm:8080/notifications # 알림이력조회 불가
```

```
{
    "timestamp": "2021-06-03T23:38:37.806+0000",
    "path": "/notifications",
    "status": 500,
    "error": "Internal Server Error",
    "message": "notification: Name does not resolve"
}
```

```
# 알림 서비스 기동
kubectl apply -f notification.yaml
```

```
(base) juwonkim@JUWONui-MacBookAir yaml % kubectl get all -n myhotel        
NAME                                READY   STATUS    RESTARTS   AGE
pod/booking-7dc6fbb847-psm2v        2/2     Running   0          11h
pod/gateway-cfc98454b-zkkz6         2/2     Running   0          15h
pod/mypage-9c855fddf-pxcgv          2/2     Running   0          11h
pod/notification-64fdcd86f5-qtsgn   2/2     Running   0          49s
pod/pay-b7b4c648b-8djqf             2/2     Running   0          11m
pod/room-7ffb788f5f-4xwmk           2/2     Running   0          11h
pod/siege                           2/2     Running   0          15h
```

```
# 알림이력 확인 (siege 사용)
http http://alarm:8080/notifications # 알림이력조회
```

```
{
    "_embedded": {
        "notifications": [
            {
                "id": null,
                "message": "\n\n##### Notify : {\"eventType\":\"PaymentApproved\",\"timestamp\":\"20210603233748\",\"id\":2,\"bookId\":5,\"roomId\":3,\"price\":1500,\"hostId\":1,\"guestId\":1,\"startDate\":1335205543511,\"endDate\":1335551143511,\"status\":\"APPROVED\"}\n\n",
                "_links": {
                    "self": {
                        "href": "http://notification:8080/notifications/1"
                    },
                    "notification": {
                        "href": "http://notification:8080/notifications/1"
                    }
                }
            }
        ]
    },
    "_links": {
        "self": {
            "href": "http://notification:8080/notifications{?page,size,sort}",
            "templated": true
        },
        "profile": {
            "href": "http://notification:8080/profile/notifications"
        }
    },
    "page": {
        "size": 20,
        "totalElements": 1,
        "totalPages": 1,
        "number": 0
    }
}
```

## Correlation 테스트

서비스를 이용해 만들어진 각 이벤트 건은 Correlation-key 연결을 통해 식별이 가능하다.

- Correlation-key로 식별하여 예약(book) 이벤트를 통해 생성된 결제(pay) 건 및 참조된 룸(room) 번호에 대해 룸 삭제 시 예약이 취소가 되고 동일한 Correlation-key를 가지는 결제(pay) 이벤트 건 역시 삭제되는 모습을 확인한다:

룸 생성을 위하여 POST 수행(3번 룸 생성)
```
{
    "price": 1000,
    "hostId": 1,
    "_links": {
        "self": {
            "href": "http://room:8080/rooms/3"
        },
        "room": {
            "href": "http://room:8080/rooms/3"
        }
    }
}
```
룸 3번을 예약하기 위해 GET 수행

```
{
    "startDate": "2012-04-23T18:25:43.511+0000",
    "endDate": "2012-04-27T18:25:43.511+0000",
    "guestId": 1,
    "hostId": 1,
    "roomId": 3,
    "status": "BOOKED",
    "price": 1500,
    "_links": {
        "self": {
            "href": "http://booking:8080/bookings/6"
        },
        "booking": {
            "href": "http://booking:8080/bookings/6"
        }
    }
}
```

결제(pay) 이벤트 건 확인을 위하여 GET 수행

```
{
                "bookId": 6,
                "roomId": 3,
                "price": 1500,
                "hostId": 1,
                "guestId": 1,
                "startDate": "2012-04-23T18:25:43.511+0000",
                "endDate": "2012-04-27T18:25:43.511+0000",
                "status": "APPROVED",
                "_links": {
                    "self": {
                        "href": "http://pay:8080/payments/3"
                    },
                    "payment": {
                        "href": "http://pay:8080/payments/3"
                    }
                }
            }
```

위 결제(pay) 이벤트 건과 동일한 식별 키를 갖는 3번 룸(room) 이벤트 건 DELETE 수행
```
http DELETE http://rooms:8080/rooms/3 # delete roomId=3
```

결제(pay) 및 예약(book) 이벤트 건을 GET 명령어를 통해 조회한 결과 룸(room)에서 삭제한 3번 키를 갖는 결제 및 예약 이벤트 또한 삭제된 것을 확인

```
{
    "_embedded": {
        "bookings": [
            {
                "startDate": "2021-06-04T00:00:00.000+0000",
                "endDate": "2021-06-05T00:00:00.000+0000",
                "guestId": 1,
                "hostId": null,
                "roomId": 1,
                "status": "BOOKED",
                "price": null,
                "_links": {
                    "self": {
                        "href": "http://booking:8080/bookings/1"
                    },
                    "booking": {
                        "href": "http://booking:8080/bookings/1"
                    }
                }
            },
            {
                "startDate": "2021-06-04T00:00:00.000+0000",
                "endDate": "2021-06-05T00:00:00.000+0000",
                "guestId": 1,
                "hostId": 1,
                "roomId": 1,
                "status": "BOOKED",
                "price": 1000,
                "_links": {
                    "self": {
                        "href": "http://booking:8080/bookings/2"
                    },
                    "booking": {
                        "href": "http://booking:8080/bookings/2"
                    }
                }
            },
            {
                "startDate": "2012-04-23T18:25:43.511+0000",
                "endDate": "2012-04-27T18:25:43.511+0000",
                "guestId": 1,
                "hostId": 1,
                "roomId": 2,
                "status": "BOOKED",
                "price": 1000,
                "_links": {
                    "self": {
                        "href": "http://booking:8080/bookings/4"
                    },
                    "booking": {
                        "href": "http://booking:8080/bookings/4"
                    }
                }
            }
        ]
    },
    "_links": {
        "self": {
            "href": "http://booking:8080/bookings{?page,size,sort}",
            "templated": true
        },
        "profile": {
            "href": "http://booking:8080/profile/bookings"
        },
        "search": {
            "href": "http://booking:8080/bookings/search"
        }
    },
    "page": {
        "size": 20,
        "totalElements": 3,
        "totalPages": 1,
        "number": 0
    }
}
```

```
{
    "_embedded": {
        "payments": [
            {
                "bookId": 4,
                "roomId": 2,
                "price": 1000,
                "hostId": 1,
                "guestId": 1,
                "startDate": "2012-04-23T18:25:43.511+0000",
                "endDate": "2012-04-27T18:25:43.511+0000",
                "status": "APPROVED",
                "_links": {
                    "self": {
                        "href": "http://pay:8080/payments/1"
                    },
                    "payment": {
                        "href": "http://pay:8080/payments/1"
                    }
                }
            }
        ]
    },
    "_links": {
        "self": {
            "href": "http://pay:8080/payments{?page,size,sort}",
            "templated": true
        },
        "profile": {
            "href": "http://pay:8080/profile/payments"
        },
        "search": {
            "href": "http://pay:8080/payments/search"
        }
    },
    "page": {
        "size": 20,
        "totalElements": 1,
        "totalPages": 1,
        "number": 0
    }
}
```

# 운영

## CI/CD 설정


각 구현체들은 각자의 source repository 에 구성되었고, 사용한 CI/CD 플랫폼은 github actions를 사용하여 git push -> mvn package -> docker build and push(aws ecr)까지 구현

main.yaml
```
# This workflow will build a Java project with Maven
# For more information see: https://help.github.com/actions/language-and-framework-guides/building-and-testing-java-with-maven
name: Java CI with Maven

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  build:

    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v2
    - name: Set up JDK 8
      uses: actions/setup-java@v2
      with:
        java-version: '8'
        distribution: 'adopt'
    - name: Build with Maven
      run: mvn package --file pom.xml
      
    - uses: actions/upload-artifact@v1
      with:
       name: mypage
       path: target/
      
  deploy:
    name: Deploy
    needs: [build]
    runs-on: ubuntu-latest
    environment: production
    
    steps:
    - name: Checkout
      uses: actions/checkout@v2
    
    - uses: actions/download-artifact@v1
      with:
          name: mypage
          path: target/

    - name: Configure AWS credentials
      uses: aws-actions/configure-aws-credentials@v1
      with:
        aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
        aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
        aws-region: ap-northeast-2

    - name: Login to Amazon ECR
      id: login-ecr
      uses: aws-actions/amazon-ecr-login@v1

    - name: Build, tag, and push image to Amazon ECR
      id: build-image
      env:
        ECR_REGISTRY: 879772956301.dkr.ecr.ap-northeast-2.amazonaws.com
        ECR_REPOSITORY: user04-mypage
        IMAGE_TAG: latest
      run: |
        # Build a docker container and
        # push it to ECR so that it can
        # be deployed to ECS.
        docker build -t $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG .
        docker push $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG
        echo "::set-output name=image::$ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG"

```

![image](https://github.com/juwarny/example-hotel-booking/blob/master/%E1%84%89%E1%85%B3%E1%84%8F%E1%85%B3%E1%84%85%E1%85%B5%E1%86%AB%E1%84%89%E1%85%A3%E1%86%BA%202021-06-04%20%E1%84%8B%E1%85%A9%E1%84%8C%E1%85%A5%E1%86%AB%208.58.24.png)
![image](https://github.com/juwarny/example-hotel-booking/blob/master/%E1%84%89%E1%85%B3%E1%84%8F%E1%85%B3%E1%84%85%E1%85%B5%E1%86%AB%E1%84%89%E1%85%A3%E1%86%BA%202021-06-04%20%E1%84%8B%E1%85%A9%E1%84%8C%E1%85%A5%E1%86%AB%209.00.21.png)


## 동기식 호출 / 서킷 브레이킹 / 장애격리

### 서킷 브레이킹 프레임워크의 선택 : istio-injection + DestinationRule

- istio-injection 적용 (기 적용 완료)
```
kubectl label namespace myhotel istio-injection=enabled --overwrite
```
- 예약, 결제 서비스 모두 아무런 변경 없음
- 부하테스터 siege 툴을 통한 서킷 브레이커 동작 확인:
- 동시사용자 255명
- 180초 동안 실시
```
$ siege -v -c255 -t180S -r10 --content-type "application/json" 'http://booking:8080/bookings POST {
    "startDate": "2012-04-23T18:25:43.511+0000",
    "endDate": "2012-04-27T18:25:43.511+0000",
    "guestId": 1,
    "hostId": 1,
    "roomId": 3,
    "price": 1500
}'
```
- 서킷브레이킹을 위한 DestinationRule 적용
```
cd myhotel/yaml
kubectl apply -f dr-pay.yaml

# dr-pay.yaml
apiVersion: networking.istio.io/v1alpha3
kind: DestinationRule
metadata:
  name: dr-pay
  namespace: myhotel
spec:
  host: pay
  trafficPolicy:
    connectionPool:
      http:
        http1MaxPendingRequests: 1
        maxRequestsPerConnection: 1
    outlierDetection:
      interval: 1s
      consecutiveErrors: 2
      baseEjectionTime: 10s
      maxEjectionPercent: 100
```

- DestinationRule 적용되어 서킷 브레이킹 동작 확인 (Kiali Graph)

![image](https://github.com/juwarny/example-hotel-booking/blob/master/%E1%84%89%E1%85%B3%E1%84%8F%E1%85%B3%E1%84%85%E1%85%B5%E1%86%AB%E1%84%89%E1%85%A3%E1%86%BA%202021-06-04%20%E1%84%8B%E1%85%A9%E1%84%8C%E1%85%A5%E1%86%AB%209.20.27.png)

- 다시 부하 발생하여 DestinationRule 적용 제거하여 정상 처리 확인
```
cd myhotel/yaml
kubectl delete -f dr-pay.yaml
```

## 오토스케일 아웃
앞서 CB 는 시스템을 안정되게 운영할 수 있게 해줬지만 사용자의 요청을 100% 받아들여주지 못했기 때문에 이에 대한 보완책으로 자동화된 확장 기능을 적용하고자 한다. 


- 예약서비스에 대한 replica 를 동적으로 늘려주도록 HPA 를 설정한다. 설정은 CPU 사용량이 5프로를 넘어서면 replica 를 10개까지 늘려준다:
```
$ kubectl autoscale deploy booking --min=1 --max=10 --cpu-percent=5 -n myhotel
```
- 오토스케일 아웃 테스트를 위하여 booking.yaml 파일 spec indent에 메모리 설정에 대한 문구를 추가한다:

```
resources:
  limits:
    cpu: 500m
  requests:
    cpu: 200m
```

- CB 에서 했던 방식대로 워크로드를 3분 동안 걸어준다.
```
$ siege -v -c255 -t180S -r10 --content-type "application/json" 'http://booking:8080/bookings POST {
    "startDate": "2012-04-23T18:25:43.511+0000",
    "endDate": "2012-04-27T18:25:43.511+0000",
    "guestId": 1,
    "hostId": 1,
    "roomId": 3,
    "price": 1500
}'
```
- 오토스케일이 어떻게 되고 있는지 모니터링을 걸어둔다:
```
kubectl get deploy booking -w -n myhotel
```
- 어느정도 시간이 흐른 후 (약 30초) 스케일 아웃이 벌어지는 것을 확인할 수 있다:
```
NAME      READY   UP-TO-DATE   AVAILABLE   AGE
booking   0/1     1            0           38s
booking   1/1     1            1           79s
booking   1/1     1            1           6m33s
booking   0/1     0            0           0s
booking   0/1     0            0           0s
booking   0/1     0            0           0s
booking   0/1     1            0           0s
booking   1/1     1            1           76s
booking   0/1     1            0           8m47s
booking   1/1     1            1           10m
booking   1/3     1            1           13m
booking   1/3     1            1           13m
booking   1/3     1            1           13m
booking   1/3     3            1           13m
booking   1/3     3            1           14m
booking   0/1     0            0           0s
booking   0/1     0            0           0s
booking   0/1     0            0           0s
booking   0/1     1            0           0s
booking   1/1     1            1           80s
booking   1/2     1            1           2m12s
booking   1/2     1            1           2m12s
booking   1/2     1            1           2m12s
booking   1/2     2            1           2m12s
booking   1/4     2            1           3m14s
booking   1/4     2            1           3m14s
booking   1/4     2            1           3m14s
booking   1/4     4            1           3m14s
booking   1/8     4            1           3m30s
booking   1/8     4            1           3m30s
booking   1/8     4            1           3m30s
booking   1/8     8            1           3m30s
booking   2/8     8            2           3m30s
booking   1/8     8            1           3m40s
booking   1/10    8            1           4m16s
booking   1/10    8            1           4m16s
booking   1/10    8            1           4m16s
booking   1/10    10           1           4m16s
booking   2/10    10           2           4m37s
booking   3/10    10           3           4m40s
booking   4/10    10           4           4m53s
booking   5/10    10           5           4m54s
booking   6/10    10           6           4m55s
booking   7/10    10           7           5m3s
booking   8/10    10           8           5m6s
booking   9/10    10           9           5m43s
booking   10/10   10           10          5m46s
```
- siege 의 로그를 보아도 전체적인 성공률이 높아진 것을 확인 할 수 있다. 
```
HTTP/1.1 201     0.06 secs:     354 bytes ==> POST http://booking:8080/bookings
HTTP/1.1 201     2.16 secs:     352 bytes ==> POST http://booking:8080/bookings
HTTP/1.1 201     0.07 secs:     354 bytes ==> POST http://booking:8080/bookings
HTTP/1.1 201     5.00 secs:     352 bytes ==> POST http://booking:8080/bookings
HTTP/1.1 201     2.37 secs:     352 bytes ==> POST http://booking:8080/bookings
HTTP/1.1 201     0.18 secs:     354 bytes ==> POST http://booking:8080/bookings
HTTP/1.1 201     0.18 secs:     354 bytes ==> POST http://booking:8080/bookings
HTTP/1.1 201     0.18 secs:     354 bytes ==> POST http://booking:8080/bookings
HTTP/1.1 201     0.10 secs:     354 bytes ==> POST http://booking:8080/bookings
HTTP/1.1 201     2.62 secs:     352 bytes ==> POST http://booking:8080/bookings
HTTP/1.1 201     0.08 secs:     354 bytes ==> POST http://booking:8080/bookings
HTTP/1.1 201     4.89 secs:     352 bytes ==> POST http://booking:8080/bookings
HTTP/1.1 201     4.68 secs:     352 bytes ==> POST http://booking:8080/bookings
HTTP/1.1 201     9.93 secs:     352 bytes ==> POST http://booking:8080/bookings
HTTP/1.1 201     0.28 secs:     354 bytes ==> POST http://booking:8080/bookings
HTTP/1.1 201     0.10 secs:     354 bytes ==> POST http://booking:8080/bookings
HTTP/1.1 201     2.26 secs:     352 bytes ==> POST http://booking:8080/bookings
HTTP/1.1 201     0.11 secs:     354 bytes ==> POST http://booking:8080/bookings
HTTP/1.1 201     2.41 secs:     352 bytes ==> POST http://booking:8080/bookings
HTTP/1.1 201     2.34 secs:     352 bytes ==> POST http://booking:8080/bookings

Lifting the server siege...
Transactions:                  18531 hits
Availability:                  99.93 %
Elapsed time:                 179.94 secs
Data transferred:               6.25 MB
Response time:                  2.44 secs
Transaction rate:             102.98 trans/sec
Throughput:                     0.03 MB/sec
Concurrency:                  251.71
Successful transactions:       18531
Failed transactions:              13
Longest transaction:           35.25
Shortest transaction:           0.01
```


## 무정지 재배포

- 먼저 무정지 재배포가 100% 되는 것인지 확인하기 위해서 Autoscaler 이나 CB 설정을 제거함
- seige 로 배포작업 직전에 워크로드를 모니터링 함.



==> Readiness 없을 경우 배포 시

```
$ siege -c1 -t60S -v http://room:8080/rooms --delay=1S

```

```
kubectl apply -f room.yaml

```

![image](https://user-images.githubusercontent.com/81946702/119099119-887ddf80-ba51-11eb-879c-e30d4819f17a.png)



- seige 의 화면으로 넘어가서 Availability 가 100% 미만으로 떨어졌는지 확인

![image](https://user-images.githubusercontent.com/81946702/119099287-b8c57e00-ba51-11eb-8a5a-991f7d3e6037.png)


==> Readiness 추가 후 배포

- 새버전으로의 배포 시작

- room.yml 에 readiessProbe 설정

```
(room.yml)
          readinessProbe:
            httpGet:
              path: '/actuator/health'
              port: 8080
            initialDelaySeconds: 10
            timeoutSeconds: 2
            periodSeconds: 5
            failureThreshold: 10
```
- room.yml 적용

```
kubectl apply -f room.yaml

```
- 동일한 시나리오로 재배포 한 후 Availability 확인:

![image](https://user-images.githubusercontent.com/81946702/119099653-16f26100-ba52-11eb-82da-f04219eabd38.png)

Availability 100%인 것 확인



## ConfigMap 사용

시스템별로 또는 운영중에 동적으로 변경 가능성이 있는 설정들을 ConfigMap을 사용하여 관리합니다.

* configmap.yaml
```
apiVersion: v1
kind: ConfigMap
metadata:
  name: myhotel-config
  namespace: myhotel
data:
  api.url.payment: http://pay:8080
  notification.prefix: Hello
```
* booking.yaml (configmap 사용)
```
...
      containers:
        - name: booking
          image: 879772956301.dkr.ecr.ap-northeast-2.amazonaws.com/user04-booking:latest
          imagePullPolicy: Always
          ports:
            - containerPort: 8080
          resources:
            limits:
              cpu: 500m
            requests:
              cpu: 200m
          env:
            - name: api.url.payment
              valueFrom:
                configMapKeyRef:
                  name: myhotel-config
                  key: api.url.payment
...

```
* kubectl describe pod/booking-7dc6fbb847-8vwtm -n myhotel
```
Containers:
  booking:
    Container ID:   docker://541140d9489d0addf1bbe13a0a0c47b978a38cb7660439c548647d1a03f9b2c8
    Image:          879772956301.dkr.ecr.ap-northeast-2.amazonaws.com/user04-booking:latest
    Image ID:       docker-pullable://879772956301.dkr.ecr.ap-northeast-2.amazonaws.com/user04-booking@sha256:120ff700c1289470af764a6029e852bcc83d3c8a2177900eec8b4a5551ee756d
    Port:           8080/TCP
    Host Port:      0/TCP
    State:          Running
      Started:      Fri, 04 Jun 2021 10:53:24 +0900
    Ready:          True
    Restart Count:  0
    Limits:
      cpu:  500m
    Requests:
      cpu:      200m
    Liveness:   http-get http://:15020/app-health/booking/livez delay=120s timeout=2s period=5s #success=1 #failure=5
    Readiness:  http-get http://:15020/app-health/booking/readyz delay=10s timeout=2s period=5s #success=1 #failure=10
    Environment:
      api.url.payment:  <set to the key 'api.url.payment' of config map 'myhotel-config'>  Optional: false
    Mounts:
      /var/run/secrets/kubernetes.io/serviceaccount from default-token-nlq2l (ro)
```

## Self-healing (Liveness Probe)

deployment.yml 에 Liveness Probe 옵션 설정

```
(book/kubernetes/deployment.yml)
          ...
          
          livenessProbe:
            httpGet:
              path: '/actuator/health'
              port: 8080
            initialDelaySeconds: 120
            timeoutSeconds: 2
            periodSeconds: 5
            failureThreshold: 5
```

retry 시도 상황 재현을 위하여 부하테스터 siege를 활용하여 book 과부하

```
siege -v -c100 -t300S --delay 2S -r10 --content-type "application/json" 'http://booking:8080/actuator/health'
```
```
siege -v -c255 -t180S -r10 --content-type "application/json" 'http://booking:8080/bookings POST {
    "startDate": "2012-04-23T18:25:43.511+0000",
    "endDate": "2012-04-27T18:25:43.511+0000",
    "guestId": 1,
    "hostId": 1,
    "roomId": 3,
    "price": 1500
}'
```
```
(base) juwonkim@JUWONui-MacBookAir yaml % kubectl get deploy booking -n myhotel -w
NAME      READY   UP-TO-DATE   AVAILABLE   AGE
booking   0/1     1            0           13s
booking   1/1     1            1           79s
booking   0/1     1            0           2m34s
booking   1/1     1            1           4m9s
```
![image](https://github.com/juwarny/example-hotel-booking/blob/master/%E1%84%89%E1%85%B3%E1%84%8F%E1%85%B3%E1%84%85%E1%85%B5%E1%86%AB%E1%84%89%E1%85%A3%E1%86%BA%202021-06-04%20%E1%84%8B%E1%85%A9%E1%84%8C%E1%85%A5%E1%86%AB%2011.25.52.png)

![image](https://github.com/juwarny/example-hotel-booking/blob/master/%E1%84%89%E1%85%B3%E1%84%8F%E1%85%B3%E1%84%85%E1%85%B5%E1%86%AB%E1%84%89%E1%85%A3%E1%86%BA%202021-06-04%20%E1%84%8B%E1%85%A9%E1%84%8C%E1%85%A5%E1%86%AB%2011.26.59.png)

## CQRS

- 게스트와 호스트가 자주 예약관리에서 확인할 수 있는 상태를 마이페이지(프론트엔드)에서 확인할 수 있어야 한다:

이벤트 생성 시 mypage에서 메시지를 통해 받는 소스코드

```
package myhotel;

import myhotel.config.kafka.KafkaProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class MyPageViewHandler {


    @Autowired
    private MyPageRepository myPageRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void whenBooked_then_CREATE_1 (@Payload Booked booked) {
        try {

            if (!booked.validate()) return;

            MyPage myPage = MyPage.builder()
                    .bookId(booked.getId())
                    .startDate(booked.getStartDate())
                    .endDate(booked.getEndDate())
                    .guestId(booked.getGuestId())
                    .hostId(booked.getHostId())
                    .price(booked.getPrice())
                    .bookStatus(booked.getStatus())
                    .roomId(booked.getRoomId())
                    .build();
            myPageRepository.save(myPage);
        
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void whenBookCanceled_then_CREATE_2 (@Payload BookCanceled bookCanceled) {
        try {

            if (!bookCanceled.validate()) return;

            MyPage myPage = MyPage.builder()
                    .bookId(bookCanceled.getId())
                    .startDate(bookCanceled.getStartDate())
                    .endDate(bookCanceled.getEndDate())
                    .guestId(bookCanceled.getGuestId())
                    .hostId(bookCanceled.getHostId())
                    .price(bookCanceled.getPrice())
                    .bookStatus(bookCanceled.getStatus())
                    .roomId(bookCanceled.getRoomId())
                    .build();
            myPageRepository.save(myPage);
        
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void whenPaymentApproved_then_CREATE_3 (@Payload PaymentApproved paymentApproved) {
        try {

            if (!paymentApproved.validate()) return;

            MyPage myPage = MyPage.builder()
                    .bookId(paymentApproved.getId())
                    .startDate(paymentApproved.getStartDate())
                    .endDate(paymentApproved.getEndDate())
                    .guestId(paymentApproved.getGuestId())
                    .hostId(paymentApproved.getHostId())
                    .price(paymentApproved.getPrice())
                    .payStatus(paymentApproved.getStatus())
                    .roomId(paymentApproved.getRoomId())
                    .build();
            myPageRepository.save(myPage);
        
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void whenPaymentCanceled_then_CREATE_4 (@Payload PaymentCanceled paymentCanceled) {
        try {

            if (!paymentCanceled.validate()) return;

            MyPage myPage = MyPage.builder()
                    .bookId(paymentCanceled.getId())
                    .startDate(paymentCanceled.getStartDate())
                    .endDate(paymentCanceled.getEndDate())
                    .guestId(paymentCanceled.getGuestId())
                    .hostId(paymentCanceled.getHostId())
                    .price(paymentCanceled.getPrice())
                    .payStatus(paymentCanceled.getStatus())
                    .roomId(paymentCanceled.getRoomId())
                    .build();
            myPageRepository.save(myPage);
        
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void whenDeletedRoom_then_CREATE_5 (@Payload DeletedRoom deletedRoom) {
        try {

            if (!deletedRoom.validate()) return;

            MyPage myPage = MyPage.builder()
                    .hostId(deletedRoom.getHostId())
                    .price(deletedRoom.getPrice())
                    .roomId(deletedRoom.getId())
                    .build();
            myPageRepository.save(myPage);
        
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void whenRegisteredRoom_then_CREATE_6 (@Payload RegisteredRoom registeredRoom) {
        try {

            if (!registeredRoom.validate()) return;
            MyPage myPage = MyPage.builder()
                    .hostId(registeredRoom.getHostId())
                    .price(registeredRoom.getPrice())
                    .roomId(registeredRoom.getId())
                    .build();
            myPageRepository.save(myPage);
        
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
```
mypage를 GET을 하여 이벤트 발생시 저장된 데이터를 출력
```
{
    "_embedded": {
        "myPages": [
            {
                "bookId": null,
                "hostId": 1,
                "price": 1000,
                "startDate": null,
                "endDate": null,
                "roomId": 1,
                "bookStatus": null,
                "payStatus": null,
                "payId": null,
                "guestId": null,
                "_links": {
                    "self": {
                        "href": "http://mypage:8080/myPages/1"
                    },
                    "myPage": {
                        "href": "http://mypage:8080/myPages/1"
                    }
                }
            },
            {
                "bookId": null,
                "hostId": 1,
                "price": 2000,
                "startDate": null,
                "endDate": null,
                "roomId": 2,
                "bookStatus": null,
                "payStatus": null,
                "payId": null,
                "guestId": null,
                "_links": {
                    "self": {
                        "href": "http://mypage:8080/myPages/2"
                    },
                    "myPage": {
                        "href": "http://mypage:8080/myPages/2"
                    }
                }
            },
            {
                "bookId": null,
                "hostId": 1,
                "price": 1000,
                "startDate": null,
                "endDate": null,
                "roomId": 3,
                "bookStatus": null,
                "payStatus": null,
                "payId": null,
                "guestId": null,
                "_links": {
                    "self": {
                        "href": "http://mypage:8080/myPages/3"
                    },
                    "myPage": {
                        "href": "http://mypage:8080/myPages/3"
                    }
                }
            },
            {
                "bookId": null,
                "hostId": 1,
                "price": 1000,
                "startDate": null,
                "endDate": null,
                "roomId": 3,
                "bookStatus": null,
                "payStatus": null,
                "payId": null,
                "guestId": null,
                "_links": {
                    "self": {
                        "href": "http://mypage:8080/myPages/4"
                    },
                    "myPage": {
                        "href": "http://mypage:8080/myPages/4"
                    }
                }
            },
            {
                "bookId": 5,
                "hostId": 1,
                "price": 1500,
                "startDate": "2012-04-23T18:25:43.511+0000",
                "endDate": "2012-04-27T18:25:43.511+0000",
                "roomId": 3,
                "bookStatus": "CANCELED",
                "payStatus": null,
                "payId": null,
                "guestId": 1,
                "_links": {
                    "self": {
                        "href": "http://mypage:8080/myPages/5"
                    },
                    "myPage": {
                        "href": "http://mypage:8080/myPages/5"
                    }
                }
            },
            {
                "bookId": 6,
                "hostId": 1,
                "price": 1500,
                "startDate": "2012-04-23T18:25:43.511+0000",
                "endDate": "2012-04-27T18:25:43.511+0000",
                "roomId": 3,
                "bookStatus": "CANCELED",
                "payStatus": null,
                "payId": null,
                "guestId": 1,
                "_links": {
                    "self": {
                        "href": "http://mypage:8080/myPages/6"
                    },
                    "myPage": {
                        "href": "http://mypage:8080/myPages/6"
                    }
                }
            }
        ]
    },
    "_links": {
        "self": {
            "href": "http://mypage:8080/myPages"
        },
        "profile": {
            "href": "http://mypage:8080/profile/myPages"
        }
    }
}
```
