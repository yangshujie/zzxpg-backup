import Cookies from 'js-cookie'

const TokenKey = 'Admin-Token'

export function getToken() {
  const token = Cookies.get(TokenKey)
  if (token === 'undefined' || token === 'null') {
    return undefined
  }
  return token
}

export function setToken(token) {
  if (token === undefined || token === null) {
    return removeToken()
  }
  return Cookies.set(TokenKey, token, { path: '/' })
}

export function removeToken() {
  return Cookies.remove(TokenKey)
}
