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


//    @StreamListener(KafkaProcessor.INPUT)
//    public void whenBookCanceled_then_UPDATE_1(@Payload BookCanceled bookCanceled) {
//        try {
//            if (!bookCanceled.validate()) return;
//                // view 객체 조회
//
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//    }
//
//    @StreamListener(KafkaProcessor.INPUT)
//    public void whenBookCanceled_then_DELETE_1(@Payload BookCanceled bookCanceled) {
//        try {
//            if (!bookCanceled.validate()) return;
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//    }
}