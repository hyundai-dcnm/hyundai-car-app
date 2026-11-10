import { useState } from 'react'

/**
 * 여행 앨범 웹. 담당: ③
 *
 * TODO(③): 초대 코드로 방 찾기 → 사진 업로드 → 차량별 사진·한 줄 소감 → 여행 카드.
 *  사진 API 는 아직 서버에 없다. ④와 contract/api.md 에 먼저 정하고 붙인다.
 */
export default function App() {
  const [code, setCode] = useState('')

  return (
    <main className="page">
      <h1>동행 앨범</h1>
      <p>같이 다녀온 차들이 찍은 사진을 한곳에 모아요.</p>
      <form className="join" onSubmit={(e) => e.preventDefault()}>
        <input
          value={code}
          onChange={(e) => setCode(e.target.value.toUpperCase())}
          placeholder="초대 코드"
          maxLength={6}
        />
        <button type="submit" disabled={code.length !== 6}>
          앨범 열기
        </button>
      </form>
    </main>
  )
}
