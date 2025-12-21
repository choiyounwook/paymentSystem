// 구매자 정보
const useremail = "dusdnr2694@naver.com"
const username = "eva"
const impcode = "imp28262115"

// 결제창 함수 넣어주기
document.getElementById('payment-button').addEventListener("click",
    paymentProcess);

var IMP = window.IMP;
var isLogin = true; // 로그인 체크 고려

// 시간 + 난수
function generateMerchantUid() {
  const now = new Date();
  return (
      now.toISOString().replace(/[-:TZ.]/g, '') +
      Math.floor(Math.random() * 1000) // 난수로 중복 방지
  );
}

function paymentProcess() {
  if (confirm("구매 하시겠습니까?")) { // 구매 확인 알림창 확인 클릭시
    if (isLogin) { // 회원만 결제 가능
      IMP.init(`${impcode}`); // 식별코드
      IMP.request_pay({
        pg: 'kakaopay.TC0ONETIME', // PG사 코드표에서 선택
        pay_method: 'card', // 결제 방식
        merchant_uid: "IMP" + generateMerchantUid(), // 결제 고유 번호
        name: '테스트', // 제품명
        amount: 100, // 결제 가격
        buyer_email: `${useremail}`, // 구매자 이메일
        buyer_name: `${username}`, // 구매자 이름
        buyer_tel: '010-1234-5678', // 구매자 번호
        buyer_addr: '서울특별시 강남구 삼성동',
        buyer_postcode: '123-456'
      }, async function (rsp) {
        console.log(rsp);
        if (rsp.success) { //결제 성공시
          // 필요한 파라미터 추가
          rsp.partnerId = 12345;  // partnerId 값 설정 (필요시 동적으로 가져올 수 있음)
          rsp.userId = 67890;     // userId 값 설정 (현재 로그인한 사용자 정보로 설정 가능)
          rsp.orderId = 112233;   // orderId 값 설정 (주문 관련 정보로 설정 가능)
          rsp.paymentDate = new Date().toISOString().split('T')[0];  // paymentDate를 현재 날짜로 설정 (yyyy-mm-dd 형식)

          console.log(rsp);

          // 백엔드 API 호출
          const response = await fetch('/api/payment/portone', {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json'
            },
            body: JSON.stringify(rsp)
          });

          const result = await response.json();
          console.log(result)

          if (rsp.status == 200) { // DB 저장 성공시
            alert('결제 완료!')
            window.location.reload();
          } else { // 결제 완료 후 DB 저장 실패시
            alert(`error:[${rsp.status}]\n결제요청이 승인된 경우 관리자에게 문의바랍니다.`);
            // DB 저장 실패시 status에 따라 추가적인 작업 가능성
          }
        } else if (rsp.success == false) { // 결제 실패시
          alert(rsp.error_msg)
        }
      });
    } else { // 비회원 결제 불가
      alert('로그인이 필요합니다.')
    }
  } else { // 구매 확인 알림창 취소 클릭시 돌아가기
    return false;
  }
}