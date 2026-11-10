// 앱(core/model)·서버(TripModels)와 같은 JSON 모양. 바꾸면 contract/api.md 도 같이 바꾼다.

export interface LatLng {
  lat: number
  lng: number
}

export interface Destination {
  name: string
  location: LatLng
}

export interface Member {
  id: string
  nickname: string
  carLabel: string
  sharingLocation: boolean
}

export interface Trip {
  id: string
  name: string
  inviteCode: string
  hostId: string
  members: Member[]
  destination: Destination | null
}
