import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type {User} from '@/types/user';

interface AuthState {
  user: User | null;
  accessToken: string | null;
  isAuthenticated: boolean;
  isAdmin: boolean;
  login: (token: string, user: User) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      accessToken: null,
      isAuthenticated: false,
      isAdmin: false,
      login: (token, user) => {
        // LocalStorage에도 별도로 저장 (Axios 인터셉터에서 사용)
        localStorage.setItem('accessToken', token);
        set({
          accessToken: token,
          user,
          isAuthenticated: true,
          isAdmin: user.role === 'ROLE_ADMIN',
        });
      },
      logout: () => {
        localStorage.removeItem('accessToken');
        set({
          user: null,
          accessToken: null,
          isAuthenticated: false,
          isAdmin: false,
        });
      },
    }),
    {
      name: 'auth-storage', // LocalStorage key
    }
  )
);
