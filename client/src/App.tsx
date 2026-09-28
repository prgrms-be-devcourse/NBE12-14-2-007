import { Link, Navigate, Route, Routes } from "react-router-dom";
import { Layout } from "./components/Layout";
import { Empty } from "./components/ui";
import { Home } from "./pages/Home";
import { Explore } from "./pages/Explore";
import { SubmissionFormPage } from "./pages/Submissions";
import { EventDetailPage } from "./pages/EventDetail";
import { Reviews, ReviewDetailPage, ReviewFormPage } from "./pages/Reviews";
import { MyPage } from "./pages/MyPage";
import { AuthPage } from "./pages/Auth";
import { AppProvider } from "./lib/context";
import { AdminReviews } from "./admin/AdminReviews";
import { AdminLogin, AdminRoot } from "./admin/AdminAuth";
import {
  AdminLayout,
  AdminDashboard,
  AdminMembers,
  AdminContent,
  AdminTickets,
  AdminActivity,
} from "./admin/Admin";

export function App() {
  return (
    <Routes>
      <Route path="admin" element={<AdminRoot />}>
        <Route path="login" element={<AdminLogin />} />
        <Route element={<AdminLayout />}>
          <Route index element={<AdminDashboard />} />
          <Route path="members" element={<AdminMembers />} />
          <Route
            path="events"
            element={<AdminContent kind="events" key="events" />}
          />
          <Route path="reviews" element={<AdminReviews />} />
          <Route path="inquiries" element={<AdminTickets />} />
          <Route path="activity" element={<AdminActivity />} />
        </Route>
      </Route>
      <Route
        element={
          <AppProvider>
            <Layout />
          </AppProvider>
        }
      >
        <Route index element={<Home />} />
        <Route path="explore" element={<Explore />} />
        <Route path="events/:eventId" element={<EventDetailPage />} />
        <Route
          path="submissions"
          element={<Navigate to="/submissions/new" replace />}
        />
        <Route path="submissions/new" element={<SubmissionFormPage />} />
        <Route path="submissions/:submissionId" element={<EventDetailPage />} />
        <Route
          path="submissions/:submissionId/edit"
          element={<SubmissionFormPage />}
        />
        <Route path="reviews" element={<Reviews />} />
        <Route path="reviews/new" element={<ReviewFormPage />} />
        <Route path="reviews/:postId" element={<ReviewDetailPage />} />
        <Route path="reviews/:postId/edit" element={<ReviewFormPage />} />
        <Route path="mypage" element={<MyPage />} />
        <Route path="login" element={<AuthPage key="login" />} />
        <Route path="signup" element={<AuthPage key="signup" signup />} />
        <Route
          path="*"
          element={
            <div className="container page-space">
              <Empty
                title="페이지를 찾을 수 없어요"
                description="주소가 바뀌었거나 존재하지 않는 페이지예요."
                action={
                  <Link className="btn primary" to="/">
                    홈으로 돌아가기
                  </Link>
                }
              />
            </div>
          }
        />
      </Route>
    </Routes>
  );
}
