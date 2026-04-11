export interface ApologeticTopicSummaryDTO {
  id: number;
  title: string;
  slug: string;
  status: string;
  responseCount: number;
  createdAt: string;
}

export interface ApologeticResponseDTO {
  id: number;
  topicId: number;
  originalPrompt: string;
  body: string;
  bodyReviewed: boolean;
  responseOrder: number;
  createdAt: string;
  updatedAt: string;
}

export interface ApologeticTopicDetailDTO {
  id: number;
  title: string;
  slug: string;
  originalPrompt: string;
  body: string;
  bodyReviewed: boolean;
  status: string;
  responses: ApologeticResponseDTO[];
  createdByEmail?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ApologeticTopicsListResponse {
  total: number;
  topics: ApologeticTopicSummaryDTO[];
}
