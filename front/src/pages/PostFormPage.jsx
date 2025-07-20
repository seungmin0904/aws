import { useEffect, useRef, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import axiosInstance from "@/lib/axiosInstance";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { useToast } from "@/hooks/use-toast";
import "@toast-ui/editor/dist/toastui-editor.css";
import { Editor } from "@toast-ui/react-editor";

const PostFormPage = ({ isEdit = false }) => {
  const { bno } = useParams();
  const navigate = useNavigate();
  const { toast } = useToast();

  const [title, setTitle] = useState("");
  const [attachmentFiles, setAttachmentFiles] = useState([]);
  const [initialContent, setInitialContent] = useState("");
  const editorRef = useRef();

  useEffect(() => {
    if (isEdit && bno) {
      axiosInstance.get(`/boards/${bno}`).then((res) => {
        const { title, content } = res.data;
        setTitle(title);
        setInitialContent(content || "");
        setTimeout(() => {
          if (editorRef.current) {
            editorRef.current.getInstance().setHTML(content || "");
          }
        }, 0);
      });
    } else {
      setTitle("");
      setInitialContent("");
      setTimeout(() => {
        if (editorRef.current) {
          editorRef.current.getInstance().setHTML("");
        }
      }, 0);
    }
  }, [isEdit, bno]);


  const uploadFile = async (file) => {
    const formData = new FormData();
    formData.append("file", file);
    const res = await axiosInstance.post("/upload", formData, {
      headers: { "Content-Type": "multipart/form-data" },
    });
    return res.data;
  };

  const imageUploadHandler = async (blob, callback) => {
    try {
      const uploadedUrl = await uploadFile(blob);
      const fullUrl = `${import.meta.env.VITE_API_BASE_URL}${uploadedUrl}`;
      callback(fullUrl, "업로드 이미지");
      toast({ title: "이미지가 본문에 삽입되었습니다." });
      // eslint-disable-next-line no-unused-vars
    } catch (error) {
      toast({
        title: "이미지 업로드 실패",
        description: "파일을 업로드할 수 없습니다.",
        variant: "destructive",
      });
    }
  };

  const handleAttachmentChange = (e) => {
    setAttachmentFiles([...e.target.files]);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    try {
      let attachments = [];
      if (!isEdit && attachmentFiles.length > 0) {
        attachments = await Promise.all(attachmentFiles.map(uploadFile));
      }
      const content = editorRef.current?.getInstance().getHTML();

      const payload = {
        title,
        content,
        ...(isEdit ? {} : { attachments }),
      };

      if (isEdit && bno) {
        await axiosInstance.put(`/boards/${bno}`, payload);
        toast({ title: "수정 완료" });
        navigate(`/posts/${bno}`);
        window.location.reload();
      } else {
        const res = await axiosInstance.post("/boards", payload);
        toast({ title: "등록 완료" });
        navigate(`/posts/${res.data.bno}`);
      }
      // eslint-disable-next-line no-unused-vars
    } catch (error) {
      
      toast({
        title: "저장 실패",
        description: "서버 오류 발생",
        variant: "destructive",
      });
    }
  };

  return (
    <div className="min-h-screen pt-24 px-4 bg-gray-50 [#18181b] flex flex-col items-center">
      <form
        onSubmit={handleSubmit}
        className="flex flex-col w-full max-w-[1200px] mx-auto space-y-8"
      >
        {/* 제목 */}
        <div>
          <Label htmlFor="title" className="mb-2 block text-lg font-semibold text-gray-900 ">제목</Label>
          <Input
            id="title"
            placeholder="제목을 입력하세요"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            className="w-full text-lg bg-white  text-gray-900  border border-zinc-200  rounded-xl"
          />
        </div>

        {/* Toast UI Editor 본문 */}
        <div>
          <Label htmlFor="content" className="mb-2 block text-lg font-semibold text-gray-900 ">내용</Label>
          <div className="w-full">
            <Editor
              ref={editorRef}
              initialValue={initialContent}
              previewStyle="vertical"
              height="900px"
              initialEditType="wysiwyg"
              useCommandShortcut={true}
              hideModeSwitch={true}
              language="ko"
              hooks={{ addImageBlobHook: imageUploadHandler }}
              placeholder="내용을 입력하세요."
              toolbarItems={[
                ["heading", "bold", "italic", "strike"],
                ["hr", "quote"],
                ["ul", "ol", "task", "indent", "outdent"],
                ["table", "image", "link"],
                ["code", "codeblock"],
              ]}
              usageStatistics={false}
              className="bg-white  border border-zinc-200  rounded-xl"
            />
          </div>
        </div>

        {/* 첨부파일 */}
        {!isEdit && (
          <div>
            <Label htmlFor="attachments" className="mb-2 block text-lg font-semibold text-gray-900 ">첨부파일</Label>
            <Input
              type="file"
              multiple
              onChange={handleAttachmentChange}
              className="w-full bg-white  text-gray-900  border border-zinc-200  rounded-xl"
            />
          </div>
        )}

        {/* 버튼 */}
        <div className="flex justify-end gap-2">
          <Button
            type="submit"
            variant="outline"
            className="rounded-xl border border-green-200  bg-green-50  text-green-500  hover:bg-green-100  hover:text-green-800  ml-2"
          >
            {isEdit ? "수정완료" : "등록하기"}
          </Button>
        </div>
      </form>
    </div>
  );
};


export default PostFormPage;


  // <div className="w-full flex justify-center pt-24 px-4">
  //   <div className="w-full max-w-5xl">
  //     <Card className="shadow-md rounded-xl">
  //       <CardHeader>
  //         <CardTitle className="text-3xl text-center">
  //           {isEdit ? "✏️ 게시글 수정" : "📝 새 게시글 작성"}
  //         </CardTitle>
  //       </CardHeader>
  //       <CardContent>
  //         <form onSubmit={handleSubmit} className="space-y-6">
  //           <div className="space-y-2">
  //             <Label htmlFor="title">제목</Label>
  //             <Input
  //               id="title"
  //               placeholder="제목을 입력하세요"
  //               value={title}
  //               onChange={(e) => setTitle(e.target.value)}
  //             />
  //           </div>

  //           <div className="space-y-2">
  //             <Label htmlFor="content">내용</Label>
  //             <Textarea
  //               id="content"
  //               placeholder="내용을 입력하세요"
  //               value={content}
  //               onChange={(e) => setContent(e.target.value)}
  //               rows={20} // ✅ 행 수 증가
  //             />
  //           </div>

  //           <Button type="submit" className="w-full text-lg py-6">
  //             {isEdit ? "수정하기" : "등록하기"}
  //           </Button>
  //         </form>
  //       </CardContent>
  //     </Card>
  //   </div>
  // </div>


